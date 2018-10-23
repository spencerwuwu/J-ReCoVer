// https://searchcode.com/api/result/15157978/

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;

import org.jsoar.kernel.events.GdsGoalRemovedEvent;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.learning.rl.ReinforcementLearningInfo;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.TemporaryMemory;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.smem.SemanticMemory;
import org.jsoar.kernel.symbols.GoalIdentifierInfo;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.BooleanPropertyProvider;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * <p>decide.cpp
 * 
 * @author ray
 */
public class Decider
{
    /**
     * The decider often needs to mark symbols with certain flags, usually to record
     * that the symbols are in certain sets or have a certain status. The
     * "common.decider_flag" field on symbols is used for this, and is set to one of
     * the following flag values. (Usually only two or three of these values are
     * used at once, and the meaning should be clear from the code.)
     * 
     * <p>decide.cpp:120:DECIDER_FLAG
     */
    public enum DeciderFlag
    {
        /** decide.cpp:120:NOTHING_DECIDER_FLAG  */
        NOTHING,
        
        /** decide.cpp:121:CANDIDATE_DECIDER_FLAG  */
        CANDIDATE,
        
        /** decide.cpp:122:CONFLICTED_DECIDER_FLAG  */
        CONFLICTED,
        
        /** decide.cpp:123:FORMER_CANDIDATE_DECIDER_FLAG  */
        FORMER_CANDIDATE,
        
        /** decide.cpp:124:BEST_DECIDER_FLAG  */
        BEST,
        
        /** decide.cpp:125:WORST_DECIDER_FLAG  */
        WORST,
        
        /** decide.cpp:126:UNARY_INDIFFERENT_DECIDER_FLAG  */
        UNARY_INDIFFERENT,
        
        /** decide.cpp:127:ALREADY_EXISTING_WME_DECIDER_FLAG  */
        ALREADY_EXISTING_WME,
        
        /** decide.cpp:132:UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG  */
        UNARY_INDIFFERENT_CONSTANT;

        /**
         * Helper to handle code that relies on NOTHING_DECIDER_FLAG being 0 in
         * boolean contexts in C (see above)
         * 
         * @return true if this flag is not NOTHING
         */
        public boolean isSomething()
        {
            return this != NOTHING;
        }
    }    
    
    /**
     * agent.h:62
     * 
     * @author ray
     */
    public enum LinkUpdateType
    {
        UPDATE_LINKS_NORMALLY,
        UPDATE_DISCONNECTED_IDS_LIST,
        JUST_UPDATE_COUNT,
    }
    
    /**
     * kernel.h:208:LOWEST_POSSIBLE_GOAL_LEVEL
     */
    private static final int LOWEST_POSSIBLE_GOAL_LEVEL = Integer.MAX_VALUE;

    /**
     * A dll of instantiations that will be used to determine the gds through a
     * backtracing-style procedure, evaluate_gds in decide.cpp
     * 
     * instantiations.h:106:pi_struct
     * 
     * @author ray
     */
    private static class ParentInstantiation
    {
        ParentInstantiation next, prev;
        Instantiation inst;
        
        public String toString()
        {
            return inst != null ? inst.toString() : "null";
        }
    }

    private static final boolean DEBUG_GDS = Boolean.valueOf(System.getProperty("jsoar.gds.debug", "false"));
    private static final boolean DEBUG_GDS_HIGH = false;
    private static final boolean DEBUG_LINKS = false;
    
    private final Agent context;
    
    // These fields are all filled in initialize() through the agent with the
    // adaptable framework. See Agent.adaptables for more info
    private PredefinedSymbols predefinedSyms;
    private DecisionManipulation decisionManip;
    private Exploration exploration;
    private Chunker chunker;
    private InputOutputImpl io;
    private DecisionCycle decisionCycle;
    private WorkingMemory workingMemory;
    private TemporaryMemory tempMemory;
    private RecognitionMemory recMemory;
    private SoarReteListener soarReteListener;
    private ReinforcementLearning rl;
    private SemanticMemory smem;
    
    /**
     * <p>gsysparam.h:164:MAX_GOAL_DEPTH
     * <p>Defaults to 100 in init_soar()
     */
    private int MAX_GOAL_DEPTH = 100;
    
    /**
     * agent.h:603:context_slots_with_changed_acceptable_preferences
     */
    private final ListHead<Slot> context_slots_with_changed_acceptable_preferences = ListHead.newInstance();
    /**
     * <p>Note: In JSoar, changed to an array list, adding ids to end and traversing
     * in reverse to maintain same behavior as push-front conses...
     * 
     * <p>agent.h:615:promoted_ids
     */
    private final List<IdentifierImpl> promoted_ids = new ArrayList<IdentifierImpl>();

    /**
     * agent.h:616:link_update_mode
     */
    private LinkUpdateType link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;
    /**
     * agent.h:609:ids_with_unknown_level
     */
    private final ListHead<IdentifierImpl> ids_with_unknown_level = ListHead.newInstance();
    /**
     * agent.h:607:disconnected_ids
     */
    private final ListHead<IdentifierImpl> disconnected_ids = ListHead.newInstance();
    
    private Marker mark_tc_number;
    private int level_at_which_marking_started;
    private int highest_level_anything_could_fall_from;
    private int lowest_level_anything_could_fall_to;
    private Marker walk_tc_number;
    private int walk_level;
    
    public IdentifierImpl top_goal;
    public IdentifierImpl bottom_goal;
    public IdentifierImpl top_state;
    public IdentifierImpl active_goal;
    IdentifierImpl previous_active_goal;
    public int active_level;
    int previous_active_level;

    // Used in new waterfall model inner preference loop
    /**
     * State for new waterfall model
     * Represents the original active level of the elaboration cycle, saved so that we can modify the active
     * level during the inner preference loop and restore it before working memory changes.
     */
	public int highest_active_level;
    /**
     * State for new waterfall model
     * Same as highest_active_level, just the goal that the level represents.
     */
	public IdentifierImpl highest_active_goal;
    /**
     * State for new waterfall model
     * Can't fire rules at this level or higher (lower int)
     */
	public int change_level;
    /**
     * State for new waterfall model
     * Next change_level, in next iteration of inner preference loop.
     */
	public int next_change_level;
    
    /**
     * agent.h:740:waitsnc
     */
    private BooleanPropertyProvider waitsnc = new BooleanPropertyProvider(SoarProperties.WAITSNC);
    
    /**
     * agent.h:384:parent_list_head
     */
    private ParentInstantiation parent_list_head;
    
    /**
     * Construct a decider using the given agent. {@link #initialize()} <b>must</b> be called.
     * 
     * @param context the owning agent
     */
    public Decider(Agent context)
    {
        Arguments.checkNotNull(context, "context");
        this.context = context;
    }
    
    public void initialize()
    {
        context.getProperties().setProvider(SoarProperties.WAITSNC, waitsnc);
        
        this.predefinedSyms = Adaptables.adapt(context, PredefinedSymbols.class);
        this.exploration = Adaptables.adapt(context, Exploration.class);
        this.decisionManip = Adaptables.adapt(context, DecisionManipulation.class);
        this.io = Adaptables.adapt(context, InputOutputImpl.class);
        this.decisionCycle = Adaptables.adapt(context, DecisionCycle.class);
        this.workingMemory = Adaptables.adapt(context, WorkingMemory.class);
        this.tempMemory = Adaptables.adapt(context, TemporaryMemory.class);
        this.recMemory = Adaptables.adapt(context, RecognitionMemory.class);
        this.soarReteListener = Adaptables.adapt(context, SoarReteListener.class);
        this.chunker = Adaptables.adapt(context, Chunker.class);
        this.rl = Adaptables.adapt(context, ReinforcementLearning.class);
        this.smem = Adaptables.require(getClass(), context, SemanticMemory.class);
    }
    
    public List<Goal> getGoalStack()
    {
        final List<Goal> result = new ArrayList<Goal>();
        for (IdentifierImpl g = top_goal; g != null; g = g.goalInfo.lower_goal)
        {
            final Goal goal = Adaptables.adapt(g, Goal.class);
            assert goal != null;
            result.add(goal);
        }
        return result;
    }
    
    /**
     * 
     * <p>chunk.cpp:753:find_goal_at_goal_stack_level
     * 
     * @param level
     * @return the goal at the given stack level
     */
    public IdentifierImpl find_goal_at_goal_stack_level(int level)
    {
        for (IdentifierImpl g = top_goal; g != null; g = g.goalInfo.lower_goal)
            if (g.level == level)
                return (g);
        return null;
    }

    /**
     * Whenever some acceptable or require preference for a context slot
     * changes, we call mark_context_slot_as_acceptable_preference_changed().
     * 
     * <p>decide.cpp:146:mark_context_slot_as_acceptable_preference_changed
     * 
     * @param s
     */
    public void mark_context_slot_as_acceptable_preference_changed(Slot s)
    {
        if (s.acceptable_preference_changed != null)
            return;

        ListItem<Slot> dc = new ListItem<Slot>(s);
        s.acceptable_preference_changed = dc;
        dc.insertAtHead(this.context_slots_with_changed_acceptable_preferences);
    } 

    /**
     * This updates the acceptable preference wmes for a single slot.
     * 
     * <p>decide.cpp:158:do_acceptable_preference_wme_changes_for_slot
     * 
     * @param s
     */
    private void do_acceptable_preference_wme_changes_for_slot(Slot s)
    {
        // first, reset marks to "NOTHING"
        for (WmeImpl w = s.getAcceptablePreferenceWmes(); w != null; w = w.next)
            w.value.decider_flag = DeciderFlag.NOTHING;

        // now mark values for which we WANT a wme as "CANDIDATE" values
        for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.CANDIDATE;
        for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.CANDIDATE;

        // remove any existing wme's that aren't CANDIDATEs; mark the rest as
        // ALREADY_EXISTING

        WmeImpl w = s.getAcceptablePreferenceWmes();
        while (w != null)
        {
            final WmeImpl next_w = w.next;
            if (w.value.decider_flag == DeciderFlag.CANDIDATE)
            {
                w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
                w.value.decider_wme = w;
                w.preference = null; /* we'll update this later */
            }
            else
            {
                s.removeAcceptablePreferenceWme(w);
                
                /*
                 * IF we lose an acceptable preference for an operator, then
                 * that operator comes out of the slot immediately in OPERAND2.
                 * However, if the lost acceptable preference is not for item in
                 * the slot, then we don;t need to do anything special until
                 * mini-quiescence.
                 */
                remove_operator_if_necessary(s, w);

                this.workingMemory.remove_wme_from_wm(w);
            }
            w = next_w;
        }

        // add the necessary wme's that don't ALREADY_EXIST

        for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next)
        {
            if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME)
            {
                // found existing wme, so just update its trace
                WmeImpl wme = p.value.decider_wme;
                if (wme.preference == null)
                    wme.preference = p;
            }
            else
            {
                WmeImpl wme = this.workingMemory.make_wme(p.id, p.attr, p.value, true);
                s.addAcceptablePreferenceWme(wme);
                wme.preference = p;
                this.workingMemory.add_wme_to_wm(wme);
                p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
                p.value.decider_wme = wme;
            }
        }

        for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next)
        {
            if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME)
            {
                // found existing wme, so just update its trace
                WmeImpl wme = p.value.decider_wme;
                if (wme.preference == null)
                    wme.preference = p;
            }
            else
            {
                WmeImpl wme = this.workingMemory.make_wme(p.id, p.attr, p.value, true);
                s.addAcceptablePreferenceWme(wme);
                wme.preference = p;
                this.workingMemory.add_wme_to_wm(wme);
                p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
                p.value.decider_wme = wme;
            }
        }
    }
    
    /**
     * 
     * <p>Moved here from consistency since it accesses no other state and is
     * only ever called from decider.
     * 
     * <p>consistency.cpp:41:remove_operator_if_necessary
     * 
     * @param s
     * @param w
     */
    private void remove_operator_if_necessary(Slot s, WmeImpl w)
    {
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer(thisAgent, &thisAgent->start_gds_tv);
        // #endif
        // #endif

        // Note: Deleted about 40 lines of commented printf debugging code here from CSoar

        if (s.getWmes() != null)
        { 
            // If there is something in the context slot
            if (s.getWmes().value == w.value)
            { 
                // The WME in the context slot is WME whose pref changed
                context.getTrace().print(Category.OPERAND2_REMOVALS,
                        "\n        REMOVING: Operator from context slot (proposal no longer matches): %s", w);
                this.remove_wmes_for_context_slot(s);
                if (s.id.goalInfo.lower_goal != null)
                {
                	context.getTrace().print(EnumSet.of(Category.VERBOSE, Category.WM_CHANGES), "Removing state %s because of an operator removal.\n", s.id.goalInfo.lower_goal);
                    this.remove_existing_context_and_descendents(s.id.goalInfo.lower_goal);
                }
            }
        }

        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        //  stop_timer(thisAgent, &thisAgent->start_gds_tv, 
        //             &thisAgent->gds_cpu_time[thisAgent->current_phase]);
        //  #endif
        //  #endif
    }

    /**
     * At the end of the phases, do_buffered_acceptable_preference_wme_changes()
     * is called to update the acceptable preference wmes. This should be called
     * *before* do_buffered_link_changes() and do_buffered_wm_changes().
     * 
     * <p>decide.cpp:232:do_buffered_acceptable_preference_wme_changes
     */
    private void do_buffered_acceptable_preference_wme_changes()
    {
        while (!context_slots_with_changed_acceptable_preferences.isEmpty())
        {
            Slot s = context_slots_with_changed_acceptable_preferences.pop();
            do_acceptable_preference_wme_changes_for_slot(s);
            s.acceptable_preference_changed = null;
        }
    }

    /**
     * Post a link addition for later processing.
     * 
     * <p>decide.cpp:288:post_link_addition
     * 
     * @param from
     * @param to
     */
    public void post_link_addition(IdentifierImpl from, IdentifierImpl to)
    {
        // don't add links to goals/impasses, except the special one (NIL,goal)
        if ((to.isGoal()) && from != null)
            return;

        to.link_count++;

        if (DEBUG_LINKS)
        {
            if (from != null)
                context.getPrinter().print("\nAdding link from %s to %s", from, to);
            else
                context.getPrinter().print("\nAdding special link to %s (count=%lu)", to, to.link_count);
        }
        
        if (from == null)
            return; /* if adding a special link, we're done */

        // if adding link from same level, ignore it
        if (from.promotion_level == to.promotion_level)
            return;

        // if adding link from lower to higher, mark higher accordingly
        if (from.promotion_level > to.promotion_level)
        {
            to.could_be_a_link_from_below = true;
            return;
        }

        // otherwise buffer it for later
        to.promotion_level = from.promotion_level;
        this.promoted_ids.add(to); // not push (see decl comment)
    }

    /**
     * decide.cpp:329:promote_if_needed
     * 
     * @param sym
     * @param new_level
     */
    private void promote_if_needed(SymbolImpl sym, int new_level)
    {
        IdentifierImpl id = sym.asIdentifier();
        if (id != null)
            promote_id_and_tc(id, new_level);
    }

    /**
     * Promote an id and its transitive closure.
     * 
     * <p>decide.cpp:333:promote_id_and_tc
     * 
     * @param id
     * @param new_level
     */
    private void promote_id_and_tc(IdentifierImpl id, /* goal_stack_level */int new_level)
    {
        // if it's already that high, or is going to be soon, don't bother
        if (id.level <= new_level)
            return;
        if (id.promotion_level < new_level)
            return;

        // update its level, etc.
        id.level = new_level;
        id.promotion_level = new_level;
        id.could_be_a_link_from_below = true;

        // sanity check
        if (id.isGoal())
        {
            throw new IllegalStateException("Internal error: tried to promote a goal or impasse id");
            /*
             * Note--since we can't promote a goal, we don't have to worry about
             * slot->acceptable_preference_wmes below
             */
        }

        // scan through all preferences and wmes for all slots for this id
        for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
            promote_if_needed(w.value, new_level);
        
        for (Slot s = id.slots; s != null; s = s.next)
        {
            for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot)
            {
                promote_if_needed(pref.value, new_level);
                if (pref.type.isBinary())
                    promote_if_needed(pref.referent, new_level);
            }
            for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                promote_if_needed(w.value, new_level);
        }
    }

    /**
     * decide.cpp:375:do_promotion
     */
    private void do_promotion()
    {
        while (!promoted_ids.isEmpty())
        {
            IdentifierImpl to = promoted_ids.remove(promoted_ids.size() - 1); // pop off end (see decl comment)
            promote_id_and_tc(to, to.promotion_level);
        }
    }

    /**
     * Post a link removal for later processing
     * 
     * decide.cpp:424:post_link_removal
     * 
     * @param from
     * @param to
     */
    public void post_link_removal(IdentifierImpl from, IdentifierImpl to)
    {
        // don't remove links to goals/impasses, except the special one
        // (NIL,goal)
        if ((to.isGoal()) && from != null)
            return;

        to.link_count--;

        if (DEBUG_LINKS)
        {
            if (from != null)
            {
                context.getPrinter().print("\nRemoving link from %s to %s (%d to %d)", from, to, from.level, to.level);
            }
            else
            {
                context.getPrinter().print("\nRemoving special link to %s  (%d)", to, to.level);
            }
            context.getPrinter().print(" (count=%lu)", to.link_count);
        }

        // if a gc is in progress, handle differently
        if (link_update_mode == LinkUpdateType.JUST_UPDATE_COUNT)
            return;

        if ((link_update_mode == LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST) && (to.link_count == 0))
        {
            if (to.unknown_level != null)
            {
                ListItem<IdentifierImpl> dc = to.unknown_level;
                dc.remove(this.ids_with_unknown_level);
                dc.insertAtHead(this.disconnected_ids);
            }
            else
            {
                to.unknown_level = new ListItem<IdentifierImpl>(to);
                to.unknown_level.insertAtHead(this.disconnected_ids);
            }
            return;
        }

        // if removing a link from a different level, there must be some
        // other link at the same level, so we can ignore this change
        if (from != null && (from.level != to.level))
            return;

        if (to.unknown_level == null)
        {
            to.unknown_level = new ListItem<IdentifierImpl>(to);
            to.unknown_level.insertAtHead(this.ids_with_unknown_level);
        }
    }

    /**
     * Garbage collect an identifier. This removes all wmes, input wmes, and
     * preferences for that id from TM.
     * 
     * decide.cpp:483:garbage_collect_id
     * 
     * @param id
     */
    private void garbage_collect_id(IdentifierImpl id)
    {
        if(DEBUG_LINKS)
        {
            context.getPrinter().print("\n*** Garbage collecting id: %s",id);
        }

        id.unknown_level = null; // From CSoar revision r10938

        // Note--for goal/impasse id's, this does not remove the impasse wme's.
        // This is handled by remove_existing_such-and-such...

        // remove any input wmes from the id
        this.workingMemory.remove_wme_list_from_wm(id.getInputWmes(), true);
        id.removeAllInputWmes();

        for (Slot s = id.slots; s != null; s = s.next)
        {
            // remove all wme's from the slot
            this.workingMemory.remove_wme_list_from_wm(s.getWmes(), false);
            s.removeAllWmes();

            // remove all preferences for the slot
            Preference pref = s.getAllPreferences();
            while (pref != null)
            {
                final Preference next_pref = pref.nextOfSlot;
                recMemory.remove_preference_from_tm(pref);

                // Note: the call to remove_preference_from_slot handles the
                // removal of acceptable_preference_wmes
                pref = next_pref;
            }

            tempMemory.mark_slot_for_possible_removal(s);
        }
    }

    /**
     * decide.cpp:545:mark_level_unknown_needed
     * 
     * @param sym
     */
    private boolean mark_level_unknown_needed(SymbolImpl sym)
    {
        return sym.asIdentifier() != null;
    }
    

    /**
     * Mark an id and its transitive closure as having an unknown level. Ids are
     * marked by setting id.tc_num to mark_tc_number. The starting id's goal
     * stack level is recorded in level_at_which_marking_started by the caller.
     * The marked ids are added to ids_with_unknown_level.
     * 
     * decide.cpp:550:mark_id_and_tc_as_unknown_level
     * 
     * @param root
     */
    private void mark_id_and_tc_as_unknown_level(IdentifierImpl root)
    {
        final Deque<IdentifierImpl> ids_to_walk = new ArrayDeque<IdentifierImpl>();
        ids_to_walk.push(root);
        
        while(!ids_to_walk.isEmpty()) {
            IdentifierImpl id = ids_to_walk.pop();
            
            // if id is already marked, do nothing
            if (id.tc_number == this.mark_tc_number)
                continue;
            
            // don't mark anything higher up as disconnected--in order to be higher
            // up, it must have a link to it up there
            if (id.level < this.level_at_which_marking_started)
                continue;
            
            // mark id, so we won't do it again later
            id.tc_number = this.mark_tc_number;
            
            // update range of goal stack levels we'll need to walk
            if (id.level < this.highest_level_anything_could_fall_from)
                this.highest_level_anything_could_fall_from = id.level;
            if (id.level > this.lowest_level_anything_could_fall_to)
                this.lowest_level_anything_could_fall_to = id.level;
            if (id.could_be_a_link_from_below)
                this.lowest_level_anything_could_fall_to = LOWEST_POSSIBLE_GOAL_LEVEL;
            
            // add id to the set of ids with unknown level
            if (id.unknown_level == null)
            {
                id.unknown_level = new ListItem<IdentifierImpl>(id);
                id.unknown_level.insertAtHead(ids_with_unknown_level);
            }
            
            // scan through all preferences and wmes for all slots for this id
            for (WmeImpl w = id.getInputWmes(); w != null; w = w.next) 
            {
                if (mark_level_unknown_needed(w.value))
                {
                    ids_to_walk.push(w.value.asIdentifier());
                }
            }
            
            for (Slot s = id.slots; s != null; s = s.next)
            {
                for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot)
                {
                    if (mark_level_unknown_needed(pref.value))
                    {
                        ids_to_walk.push(pref.value.asIdentifier());
                    }
                    if (pref.type.isBinary())
                    {
                        if (mark_level_unknown_needed(pref.referent))
                        {
                            ids_to_walk.push(pref.referent.asIdentifier());
                        }
                    }
                }
                if (s.impasse_id != null)
                {
                    if (mark_level_unknown_needed(s.impasse_id))
                    {
                        ids_to_walk.push(s.impasse_id.asIdentifier());
                    }
                }
                for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    if (mark_level_unknown_needed(w.value))
                    {
                        ids_to_walk.push(w.value.asIdentifier());
                    }
                }
            } /* end of for slots loop */
        }
    }

    /**
     * decide.cpp:647:level_update_needed
     * 
     * @param sym
     */
    private boolean level_update_needed(SymbolImpl sym)
    {
        IdentifierImpl id = sym.asIdentifier();
        return id != null && id.tc_number != this.walk_tc_number;
    }

    /**
     * After marking the ids with unknown level, we walk various levels of the
     * goal stack, higher level to lower level. If, while doing the walk, we
     * encounter an id marked as having an unknown level, we update its level
     * and remove it from ids_with_unknown_level.
     * 
     * decide.cpp:652:walk_and_update_levels
     * 
     * @param root
     */
    private void walk_and_update_levels(IdentifierImpl root)
    {
        Deque<IdentifierImpl> ids_to_walk = new ArrayDeque<IdentifierImpl>();
        ids_to_walk.push(root);
        
        while(!ids_to_walk.isEmpty()) {
            IdentifierImpl id = ids_to_walk.pop();

            // mark id so we don't walk it twice
            id.tc_number = this.walk_tc_number;
            
            // if we already know its level, and it's higher up, then exit
            if ((id.unknown_level == null) && (id.level < this.walk_level))
                continue;
            
            // if we didn't know its level before, we do now
            if (id.unknown_level != null)
            {
                id.unknown_level.remove(this.ids_with_unknown_level);
                id.unknown_level = null;
                id.level = this.walk_level;
                id.promotion_level = this.walk_level;
            }
            
            // scan through all preferences and wmes for all slots for this id
            for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
            {
                if (level_update_needed(w.value))
                {
                    ids_to_walk.push(w.value.asIdentifier());
                }
            }
            for (Slot s = id.slots; s != null; s = s.next)
            {
                for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot)
                {
                    if (level_update_needed(pref.value))
                    {
                        ids_to_walk.push(pref.value.asIdentifier());
                    }
                    if (pref.type.isBinary())
                    {
                        if (level_update_needed(pref.referent))
                        {
                            ids_to_walk.push(pref.referent.asIdentifier());
                        }
                    }
                }
                if (s.impasse_id != null)
                {
                    if (level_update_needed(s.impasse_id))
                    {
                        ids_to_walk.push(s.impasse_id.asIdentifier());
                    }
                }
                for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    if (level_update_needed(w.value))
                    {
                        ids_to_walk.push(w.value.asIdentifier());
                    }
                }
            } /* end of for slots loop */
        }
    }

    /**
     * Do all buffered demotions and gc's.
     * 
     * decide.cpp:666:do_demotion
     */
    private void do_demotion()
    {
        // scan through ids_with_unknown_level, move the ones with link_count==0
        // over to disconnected_ids
        ListItem<IdentifierImpl> dc, next_dc;
        for (dc = ids_with_unknown_level.first; dc != null; dc = next_dc)
        {
            next_dc = dc.next;
            final IdentifierImpl id = dc.item;
            if (id.link_count == 0)
            {
                dc.remove(this.ids_with_unknown_level);
                dc.insertAtHead(this.disconnected_ids);
            }
        }

        // keep garbage collecting ids until nothing left to gc
        this.link_update_mode = LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST;
        while (!this.disconnected_ids.isEmpty())
        {
            final IdentifierImpl id = disconnected_ids.pop();
            garbage_collect_id(id);
        }
        this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;

        // if nothing's left with an unknown level, we're done
        if (this.ids_with_unknown_level.isEmpty())
            return;

        // do the mark
        this.highest_level_anything_could_fall_from = LOWEST_POSSIBLE_GOAL_LEVEL;
        this.lowest_level_anything_could_fall_to = -1;
        this.mark_tc_number = DefaultMarker.create();
        for (dc = this.ids_with_unknown_level.first; dc != null; dc = dc.next)
        {
            final IdentifierImpl id = dc.item;
            this.level_at_which_marking_started = id.level;
            mark_id_and_tc_as_unknown_level(id);
        }

        // do the walk
        IdentifierImpl g = this.top_goal;
        while (true)
        {
            if (g == null)
                break;
            if (g.level > this.lowest_level_anything_could_fall_to)
                break;
            if (g.level >= this.highest_level_anything_could_fall_from)
            {
                this.walk_level = g.level;
                this.walk_tc_number = DefaultMarker.create();
                walk_and_update_levels(g);
            }
            g = g.goalInfo.lower_goal;
        }

        // GC anything left with an unknown level after the walk
        this.link_update_mode = LinkUpdateType.JUST_UPDATE_COUNT;
        while (!ids_with_unknown_level.isEmpty())
        {
            final IdentifierImpl id = ids_with_unknown_level.pop();
            garbage_collect_id(id);
        }
        this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;
    }

    /**
     * This routine does all the buffered link (ownership) changes, updating the
     * goal stack level on all identifiers and garbage collecting disconnected
     * wmes.
     * 
     * <p>decide.cpp:744:do_buffered_link_changes
     */
    private void do_buffered_link_changes()
    {
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // struct timeval saved_start_tv;
        // #endif
        // #endif

        // if no promotions or demotions are buffered, do nothing
        if (promoted_ids.isEmpty() && ids_with_unknown_level.isEmpty() && disconnected_ids.isEmpty())
            return;

        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer (thisAgent, &saved_start_tv);
        // #endif
        // #endif
        do_promotion();
        do_demotion();
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        //  stop_timer (thisAgent, &saved_start_tv, &thisAgent->ownership_cpu_time[thisAgent->current_phase]);
        //#endif
        //#endif
    }


    /**
     * Require_preference_semantics() is a helper function for
     * run_preference_semantics() that is used when there is at least one
     * require preference for the slot.
     * 
     * decide.cpp:803:require_preference_semantics
     * 
     * @return
     */
    private ImpasseType require_preference_semantics(Slot s, ByRef<Preference> result_candidates, boolean consistency)
    {
        // collect set of required items into candidates list
        for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.NOTHING;
        
        Preference candidates = null;
        for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next)
        {
            if (p.value.decider_flag == DeciderFlag.NOTHING)
            {
                p.next_candidate = candidates;
                candidates = p;
                // unmark it, in order to prevent it from being added twice
                p.value.decider_flag = DeciderFlag.CANDIDATE;
            }
        }
        result_candidates.value = candidates;

        // if more than one required item, we have a constraint failure
        if (candidates.next_candidate != null)
            return ImpasseType.CONSTRAINT_FAILURE;

        // just one require, check for require-prohibit impasse
        SymbolImpl value = candidates.value;
        for (Preference p = s.getPreferencesByType(PreferenceType.PROHIBIT); p != null; p = p.next)
            if (p.value == value)
                return ImpasseType.CONSTRAINT_FAILURE;

        // the lone require is the winner
        if (!consistency && candidates != null && rl.rl_enabled())
        {
            rl.rl_tabulate_reward_values();
            exploration.exploration_compute_value_of_candidate( candidates, s, 0 );
            rl.rl_perform_update( candidates.numeric_value, candidates.rl_contribution, s.id );
        }

        return ImpasseType.NONE;
    }
    

    /**
     * Examines the preferences for a given slot, and returns an impasse type
     * for the slot. The argument "result_candidates" is set to a list of
     * candidate values for the slot--if the returned impasse type is
     * NONE_IMPASSE_TYPE, this is the set of winners; otherwise it is the set of
     * tied, conflicted, or constraint-failured values. This list of values is a
     * list of preferences for those values, linked via the "next_candidate"
     * field on each preference structure. If there is more than one preference
     * for a given value, only one is returned in the result_candidates, with
     * (first) require preferences being preferred over acceptable preferences,
     * and (second) preferences from higher match goals being preferred over
     * those from lower match goals.
     * 
     * <p>BUGBUG There is a problem here: since the require/acceptable priority
     * takes precedence over the match goal level priority, it's possible that
     * we could return a require preference from lower in the goal stack than
     * some acceptable preference. If the goal stack gets popped soon afterwards
     * (i.e., before the next time the slot is re-decided, I think), we would be
     * left with a WME still in WM (not GC'd, because of the acceptable
     * preference higher up) but with a trace pointing to a deallocated require
     * preference. This case is very obsure and unlikely to come up, but it
     * could easily cause a core dump or worse.
     * 
     * <p>decide.cpp:840:run_preference_semantics
     * 
     * @param s
     * @param result_candidates
     * @param consistency (defaulted to false in CSoar)
     * @param predict  (defaulted to false in CSoar)
     * @return
     */
    private ImpasseType run_preference_semantics(Slot s, ByRef<Preference> result_candidates,
            boolean consistency /* = false */, boolean predict /* = false */)
    {
        // if the slot has no preferences at all, things are trivial
        if (s.getAllPreferences() == null)
        {
            if (!s.isa_context_slot)
                tempMemory.mark_slot_for_possible_removal(s);
            result_candidates.value = null;
            return ImpasseType.NONE;
        }

        // if this is the true decision slot and selection has been made,
        // attempt force selection
        if (!s.isa_context_slot && !consistency)
        {
            if (decisionManip.select_get_operator() != null)
            {
                final Preference force_result = decisionManip.select_force(s.getPreferencesByType(PreferenceType.ACCEPTABLE), !predict);

                if (force_result != null)
                {
                    force_result.next_candidate = null;
                    result_candidates.value = force_result;

                    if (!predict && rl.rl_enabled())
                    {
                        rl.rl_tabulate_reward_values();
                        exploration.exploration_compute_value_of_candidate( force_result, s, 0 );
                        rl.rl_perform_update( force_result.numeric_value, force_result.rl_contribution, s.id );
                    }

                    return ImpasseType.NONE;
                }
                else
                {
                    context.getPrinter().warn( "WARNING: Invalid forced selection operator id" );
                }
            }
        }

        /* === Requires === */
        if (s.getPreferencesByType(PreferenceType.REQUIRE) != null)
        {
            return require_preference_semantics(s, result_candidates, consistency);
        }

        /* === Acceptables, Prohibits, Rejects === */

        // mark everything that's acceptable, then unmark the prohibited and rejected items
        for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.CANDIDATE;
        for (Preference p = s.getPreferencesByType(PreferenceType.PROHIBIT); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.NOTHING;
        for (Preference p = s.getPreferencesByType(PreferenceType.REJECT); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.NOTHING;

        // now scan through acceptables and build the list of candidates
        Preference candidates = null;
        for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next)
        {
            if (p.value.decider_flag == DeciderFlag.CANDIDATE)
            {
                p.next_candidate = candidates;
                candidates = p;
                // unmark it, in order to prevent it from being added twice
                p.value.decider_flag = DeciderFlag.NOTHING;
            }
        }

        /* === Handling of attribute_preferences_mode 2 === */
        if (!s.isa_context_slot)
        {
            result_candidates.value = candidates;
            return ImpasseType.NONE;
        }

        /* === If there are only 0 or 1 candidates, we're done === */
        if ((candidates == null) || (candidates.next_candidate == null))
        {
            result_candidates.value = candidates;

            if (!consistency && rl.rl_enabled() && candidates != null)
            {
                // perform update here for just one candidate
                rl.rl_tabulate_reward_values();
                exploration.exploration_compute_value_of_candidate(candidates, s, 0);
                rl.rl_perform_update( candidates.numeric_value, candidates.rl_contribution, s.id );
            }

            return ImpasseType.NONE;
        }

        /* === Better/Worse === */
        if (s.getPreferencesByType(PreferenceType.BETTER) != null
                || s.getPreferencesByType(PreferenceType.WORSE) != null)
        {
            // algorithm:
            // for each j > k:
            //   if j is (candidate or conflicted) and k is (candidate or conflicted):
            //     if one of (j, k) is candidate:
            //       candidate -= k, if not already true
            //       conflicted += k, if not already true
            // for each j < k:
            //   if j is (candidate or conflicted) and k is (candidate or conflicted):
            //     if one of (j, k) is candidate:
            //       candidate -= j, if not already true
            //       conflicted += j, if not already true
            // if no remaining candidates:
            //   conflict impasse using conflicted as candidates
            // else
            //   pass on candidates to next filter
            for (Preference p = s.getPreferencesByType(PreferenceType.BETTER); p != null; p = p.next)
            {
                p.value.decider_flag = DeciderFlag.NOTHING;
                p.referent.decider_flag = DeciderFlag.NOTHING;
            }
            for (Preference p = s.getPreferencesByType(PreferenceType.WORSE); p != null; p = p.next)
            {
                p.value.decider_flag = DeciderFlag.NOTHING;
                p.referent.decider_flag = DeciderFlag.NOTHING;
            }
            for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            {
                cand.value.decider_flag = DeciderFlag.CANDIDATE;
            }
            for (Preference p = s.getPreferencesByType(PreferenceType.BETTER); p != null; p = p.next)
            {
                final SymbolImpl j = p.value;
                final SymbolImpl k = p.referent;
                if (j == k)
                    continue;
                if (j.decider_flag.isSomething() && k.decider_flag.isSomething())
                {
                    // decide.cpp:1044: changes from old algorithm
                    if (j.decider_flag == DeciderFlag.CANDIDATE || k.decider_flag == DeciderFlag.CANDIDATE)
                        k.decider_flag = DeciderFlag.CONFLICTED;
                }
            }
            
            for (Preference p = s.getPreferencesByType(PreferenceType.WORSE); p != null; p = p.next)
            {
                final SymbolImpl j = p.value;
                final SymbolImpl k = p.referent;
                if (j == k)
                    continue;
                if (j.decider_flag.isSomething() && k.decider_flag.isSomething())
                {
                    // decide.cpp:1057: changes from old algorithm
                    if (j.decider_flag == DeciderFlag.CANDIDATE || k.decider_flag == DeciderFlag.CANDIDATE)
                        j.decider_flag = DeciderFlag.CONFLICTED;
                }
            }

            // now scan through candidates list, look for remaining candidates
            // decide.cpp:1063: collecting candidates now, not conflicts as in old algorithm
            Preference cand = null, prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
            {
                if (cand.value.decider_flag == DeciderFlag.CANDIDATE)
                    break;
            }
            if (cand == null)
            {
                // collect conflicted candidates into new candidates list
                prev_cand = null;
                cand = candidates;
                while (cand != null)
                {
                    if (cand.value.decider_flag != DeciderFlag.CONFLICTED)
                    {
                        if (prev_cand != null)
                            prev_cand.next_candidate = cand.next_candidate;
                        else
                            candidates = cand.next_candidate;
                    }
                    else
                    {
                        prev_cand = cand;
                    }
                    cand = cand.next_candidate;
                }
                result_candidates.value = candidates;
                return ImpasseType.CONFLICT;
            }

            // non-conflict candidates found, remove conflicts from candidates
            prev_cand = null;
            cand = candidates;
            while (cand != null)
            {
                if (cand.value.decider_flag == DeciderFlag.CONFLICTED)
                {
                    if (prev_cand != null)
                        prev_cand.next_candidate = cand.next_candidate;
                    else
                        candidates = cand.next_candidate;
                }
                else
                {
                    prev_cand = cand;
                }
                cand = cand.next_candidate;
            }
        }
    

        /* === Bests === */
        if (s.getPreferencesByType(PreferenceType.BEST) != null)
        {
            Preference cand, prev_cand;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                cand.value.decider_flag = DeciderFlag.NOTHING;
            for (Preference p = s.getPreferencesByType(PreferenceType.BEST); p != null; p = p.next)
                p.value.decider_flag = DeciderFlag.BEST;
            prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                if (cand.value.decider_flag == DeciderFlag.BEST)
                {
                    if (prev_cand != null)
                        prev_cand.next_candidate = cand;
                    else
                        candidates = cand;
                    prev_cand = cand;
                }
            if (prev_cand != null)
                prev_cand.next_candidate = null;
        }

        /* === Worsts === */
        if (s.getPreferencesByType(PreferenceType.WORST) != null)
        {
            Preference cand, prev_cand;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                cand.value.decider_flag = DeciderFlag.NOTHING;
            for (Preference p = s.getPreferencesByType(PreferenceType.WORST); p != null; p = p.next)
                p.value.decider_flag = DeciderFlag.WORST;
            prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                if (cand.value.decider_flag != DeciderFlag.WORST)
                {
                    if (prev_cand != null)
                        prev_cand.next_candidate = cand;
                    else
                        candidates = cand;
                    prev_cand = cand;
                }
            if (prev_cand != null)
                prev_cand.next_candidate = null;
        }

        /* === If there are only 0 or 1 candidates, we're done === */
        if (candidates == null || candidates.next_candidate == null)
        {
            result_candidates.value = candidates;

            if (!consistency && rl.rl_enabled() && candidates != null)
            {
                // perform update here for just one candidate
                rl.rl_tabulate_reward_values();
                exploration.exploration_compute_value_of_candidate( candidates, s, 0 );
                rl.rl_perform_update( candidates.numeric_value, candidates.rl_contribution, s.id );
            }

            return ImpasseType.NONE;
        }

        /* === Indifferents === */
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.NOTHING;
        for (Preference p = s.getPreferencesByType(PreferenceType.UNARY_INDIFFERENT); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT;

        for (Preference p = s.getPreferencesByType(PreferenceType.NUMERIC_INDIFFERENT); p != null; p = p.next)
            p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT;

        for (Preference p = s.getPreferencesByType(PreferenceType.BINARY_INDIFFERENT); p != null; p = p.next)
        {
            if ((p.referent.asInteger() != null) || (p.referent.asDouble() != null))
                p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT;
        }

        boolean not_all_indifferent = false;
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
        {
            if (cand.value.decider_flag == DeciderFlag.UNARY_INDIFFERENT)
                continue;
            else if (cand.value.decider_flag == DeciderFlag.UNARY_INDIFFERENT_CONSTANT)
                continue;

            // check whether cand is binary indifferent to each other one
            for (Preference p = candidates; p != null; p = p.next_candidate)
            {
                if (p == cand)
                    continue;
                boolean match_found = false;
                for (Preference p2 = s.getPreferencesByType(PreferenceType.BINARY_INDIFFERENT);
                         p2 != null; p2 = p2.next)
                {
                    if (((p2.value == cand.value) && (p2.referent == p.value))
                            || ((p2.value == p.value) && (p2.referent == cand.value)))
                    {
                        match_found = true;
                        break;
                    }
                }
                if (!match_found)
                {
                    not_all_indifferent = true;
                    break;
                }
            } /* end of for p loop */
            if (not_all_indifferent)
                break;
        } /* end of for cand loop */

        if (!not_all_indifferent)
        {
            if (!consistency)
            {
                result_candidates.value = exploration.exploration_choose_according_to_policy(s, candidates);
                result_candidates.value.next_candidate = null;
            }
            else
                result_candidates.value = candidates;

            return ImpasseType.NONE;
        }

        // items not all indifferent; for context slots this gives a tie
        if (s.isa_context_slot)
        {
            result_candidates.value = candidates;
            return ImpasseType.TIE;
        }

        result_candidates.value = candidates;

        // otherwise we have a tie
        return ImpasseType.TIE;
    }
    
    /**
     * decide.cpp:1204:run_preference_semantics_for_consistency_check
     * 
     * @param s
     * @param result_candidates
     * @return the impasse type
     */
    public ImpasseType run_preference_semantics_for_consistency_check (Slot s, ByRef<Preference> result_candidates) 
    {
        return run_preference_semantics(s, result_candidates, true, false );
    }
    
    /**
     * This creates a new wme and adds it to the given impasse object. "Id"
     * indicates the goal/impasse id; (id ^attr value) is the impasse wme to be
     * added. The "preference" argument indicates the preference (if non-NIL)
     * for backtracing.
     * 
     * decide.cpp:1224:add_impasse_wme
     * 
     * @param id
     * @param attr
     * @param value
     * @param p
     */
    private void add_impasse_wme(IdentifierImpl id, SymbolImpl attr, SymbolImpl value, Preference p)
    {
        WmeImpl w = this.workingMemory.make_wme(id, attr, value, false);
        id.goalInfo.addImpasseWme(w);
        w.preference = p;
        this.workingMemory.add_wme_to_wm(w);
    }

    /**
     * This creates a new impasse, returning its identifier. The caller is
     * responsible for filling in either id->isa_impasse or id->isa_goal, and
     * all the extra stuff for goal identifiers.
     * 
     * decide.cpp:1241:create_new_impasse
     * 
     * @param goalInfo
     * @param object
     * @param attr
     * @param impasse_type
     * @param level
     *            Goal stack level
     * @return
     */
    private IdentifierImpl create_new_impasse(SymbolImpl object, SymbolImpl attr, ImpasseType impasse_type, int level)
    {
        final PredefinedSymbols predefined = predefinedSyms; // reduce typing
        
        final IdentifierImpl id = predefined.getSyms().make_new_identifier('S', level);
        post_link_addition(null, id); // add the special link

        id.goalInfo = new GoalIdentifierInfo(id);

        add_impasse_wme(id, predefined.type_symbol, predefined.state_symbol, null);
        add_impasse_wme(id, predefined.superstate_symbol, object, null);

        if (attr != null)
        {
            add_impasse_wme(id, predefined.attribute_symbol, attr, null);
        }

        switch (impasse_type)
        {
        case NONE: // this happens only when creating the top goal
            break; 
        case CONSTRAINT_FAILURE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.constraint_failure_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.none_symbol, null);
            break;
        case CONFLICT:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.conflict_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.multiple_symbol, null);
            break;
        case TIE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.tie_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.multiple_symbol, null);
            break;
        case NO_CHANGE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.no_change_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.none_symbol, null);
            break;
        }
        
        id.goalInfo.allow_bottom_up_chunks = true;
        id.goalInfo.operator_slot = Slot.make_slot(id, predefinedSyms.operator_symbol, predefinedSyms.operator_symbol);

        // Create RL link
        id.goalInfo.reward_header = predefined.getSyms().make_new_identifier('R', level);
        SoarModule.add_module_wme(workingMemory, id, predefined.rl_sym_reward_link, id.goalInfo.reward_header);
        
        // Create SMEM stuff
        smem.initializeNewContext(workingMemory, id);

        return id;
    }
    
    /**
     * Fake Preferences for Goal ^Item Augmentations
     * 
     * <p>When we backtrace through a (goal ^item) augmentation, we want to
     * backtrace to the acceptable preference wme in the supercontext
     * corresponding to that ^item. A slick way to do this automagically is to
     * set the backtracing preference pointer on the (goal ^item) wme to be a
     * "fake" preference for a "fake" instantiation. The instantiation has as
     * its LHS a list of one condition, which matched the acceptable preference
     * wme in the supercontext.
     * 
     * <p>Make_fake_preference_for_goal_item() builds such a fake preference and
     * instantiation, given a pointer to the supergoal and the
     * acceptable/require preference for the value, and returns a pointer to the
     * fake preference. *** for Soar 8.3, we changed the fake preference to be
     * ACCEPTABLE instead of REQUIRE. This could potentially break some code,
     * but it avoids the BUGBUG condition that can occur when you have a REQUIRE
     * lower in the stack than an ACCEPTABLE but the goal stack gets popped
     * while the WME backtrace still points to the REQUIRE, instead of the
     * higher ACCEPTABLE. See the section above on Preference Semantics. It also
     * allows the GDS to backtrace through ^items properly.
     * 
     * <p>decide.cpp:1350:make_fake_preference_for_goal_item
     * 
     * @param goal
     * @param cand
     * @return
     */
    private Preference make_fake_preference_for_goal_item(IdentifierImpl goal, Preference cand)
    {
        // find the acceptable preference wme we want to backtrace to
        final Slot s = cand.slot;
        WmeImpl ap_wme;
        for (ap_wme = s.getAcceptablePreferenceWmes(); ap_wme != null; ap_wme = ap_wme.next)
            if (ap_wme.value == cand.value)
                break;
        if (ap_wme == null)
        {
            throw new IllegalStateException("Internal error: couldn't find acceptable pref wme");
        }
        // make the fake preference
        final Preference pref = new Preference(PreferenceType.ACCEPTABLE, goal,
                predefinedSyms.item_symbol, cand.value, null);
        goal.goalInfo.addGoalPreference(pref);
        pref.on_goal_list = true;
        pref.preference_add_ref();

        // make the fake instantiation
        final Instantiation inst = new Instantiation(null, null, null);
        pref.setInstantiation(inst);
        inst.match_goal = goal;
        inst.match_goal_level = goal.level;
        inst.okay_to_variablize = true;
        inst.backtrace_number = 0;
        inst.in_ms = false;

        // make the fake condition
        final PositiveCondition cond = new PositiveCondition();

        cond.id_test = SymbolImpl.makeEqualityTest(ap_wme.id); // make_equality_test
                                                            // (ap_wme->id);
        cond.attr_test = SymbolImpl.makeEqualityTest(ap_wme.attr);
        cond.value_test = SymbolImpl.makeEqualityTest(ap_wme.value);
        cond.test_for_acceptable_preference = true;
        cond.bt().wme_ = ap_wme;
        cond.bt().level = ap_wme.id.level;
        
        inst.top_of_instantiated_conditions = cond;
        inst.bottom_of_instantiated_conditions = cond;
        inst.nots = null;
        
        if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
        {
            // (removed in jsoar) ap_wme.wme_add_ref();
        }
        else
        {
            if (inst.match_goal_level > SoarConstants.TOP_GOAL_LEVEL)
            {
             // (removed in jsoar) ap_wme.wme_add_ref();
            }
        }

        // return the fake preference
        return pref;
    }

    /**
     * Remove_fake_preference_for_goal_item() is called to clean up the fake
     * stuff once the (goal ^item) wme is no longer needed.
     * 
     * decide.cpp:1419:remove_fake_preference_for_goal_item
     * 
     * @param pref
     */
    private void remove_fake_preference_for_goal_item(Preference pref)
    {
        pref.preference_remove_ref(recMemory); /* everything else happens automatically */
    }
    
    /**
     * This routine updates the set of ^item wmes on a goal or attribute
     * impasse. It takes the identifier of the goal/impasse, and a list of
     * preferences (linked via the "next_candidate" field) for the new set of
     * items that should be there.
     * 
     * decide.cpp:1432:update_impasse_items
     * 
     * @param id
     * @param items
     */
    private void update_impasse_items(IdentifierImpl id, Preference items)
    {
        /*
        Count up the number of candidates
        REW: 2003-01-06
        I'm assuming that all of the candidates have unary or 
        unary+value (binary) indifferent preferences at this point.
        So we loop over the candidates list and count the number of
        elements in the list.
        */
        final int item_count = Preference.countCandidates(items);

        // reset flags on existing items to "NOTHING"
        for (WmeImpl w = id.goalInfo.getImpasseWmes(); w != null; w = w.next)
            if (w.attr == predefinedSyms.item_symbol)
                w.value.decider_flag = DeciderFlag.NOTHING;

        // mark set of desired items as "CANDIDATEs"
        for (Preference cand = items; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.CANDIDATE;

        // for each existing item: if it's supposed to be there still, then
        // mark it "ALREADY_EXISTING"; otherwise remove it
        WmeImpl w = id.goalInfo.getImpasseWmes();
        while (w != null)
        {
            final WmeImpl next_w = w.next;
            if (w.attr == predefinedSyms.item_symbol)
            {
                if (w.value.decider_flag == DeciderFlag.CANDIDATE)
                {
                    w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
                    w.value.decider_wme = w; // so we can update the pref later
                }
                else
                {
                    id.goalInfo.removeImpasseWme(w);
                    remove_fake_preference_for_goal_item(w.preference);
                    this.workingMemory.remove_wme_from_wm(w);
                }
            }

            // SBW 5/07
            // remove item-count WME if it exists
            else if (w.attr == predefinedSyms.item_count_symbol)
            {
                id.goalInfo.removeImpasseWme(w);
                this.workingMemory.remove_wme_from_wm(w);
            }

            w = next_w;
        }

        // for each desired item: if it doesn't ALREADY_EXIST, add it
        for (Preference cand = items; cand != null; cand = cand.next_candidate)
        {
            Preference bt_pref;
            if (id.isGoal())
                bt_pref = make_fake_preference_for_goal_item(id, cand);
            else
                bt_pref = cand;
            if (cand.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME)
            {
                if (id.isGoal())
                    remove_fake_preference_for_goal_item(cand.value.decider_wme.preference);
                cand.value.decider_wme.preference = bt_pref;
            }
            else
            {
                add_impasse_wme(id, predefinedSyms.item_symbol, cand.value, bt_pref);
            }
        }

        // SBW 5/07
        // update the item-count WME
        // detect relevant impasses by having more than one item
        if (item_count > 0)
        {
            add_impasse_wme(id, predefinedSyms.item_count_symbol, predefinedSyms.getSyms().createInteger(item_count), null);
        }
    }

    
    /**
     * This routine decides a given slot, which must be a non-context 
