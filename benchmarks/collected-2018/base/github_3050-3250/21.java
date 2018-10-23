// https://searchcode.com/api/result/96821422/

package me.blackvein.quests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.blackvein.quests.prompts.ItemStackPrompt;
import me.blackvein.quests.util.CK;
import me.blackvein.quests.util.ColorUtil;
import me.blackvein.quests.util.ItemUtil;
import me.blackvein.quests.util.Lang;
import me.blackvein.quests.util.QuestMob;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EventFactory implements ConversationAbandonedListener, ColorUtil {

    Quests              quests;
    Map<Player, Quest>  editSessions               = new HashMap<Player, Quest>();
    Map<Player, Block>  selectedExplosionLocations = new HashMap<Player, Block>();
    Map<Player, Block>  selectedEffectLocations    = new HashMap<Player, Block>();
    Map<Player, Block>  selectedMobLocations       = new HashMap<Player, Block>();
    Map<Player, Block>  selectedLightningLocations = new HashMap<Player, Block>();
    Map<Player, Block>  selectedTeleportLocations  = new HashMap<Player, Block>();
    List<String>        names                      = new LinkedList<String>();
    ConversationFactory convoCreator;
    File                eventsFile;

    public EventFactory(Quests plugin) {

        quests = plugin;

        // Ensure to initialize convoCreator last, to ensure that 'this' is
        // fully initialized before it is passed
        convoCreator = new ConversationFactory(plugin).withModality(false).withLocalEcho(false)
                .withPrefix(new QuestCreatorPrefix()).withFirstPrompt(new MenuPrompt()).withTimeout(3600)
                .thatExcludesNonPlayersWithMessage("Console may not perform this operation!")
                .addConversationAbandonedListener(this);

    }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {

        final Player player = (Player) abandonedEvent.getContext().getForWhom();
        selectedExplosionLocations.remove(player);
        selectedEffectLocations.remove(player);
        selectedMobLocations.remove(player);
        selectedLightningLocations.remove(player);
        selectedTeleportLocations.remove(player);

    }

    private class QuestCreatorPrefix implements ConversationPrefix {

        @Override
        public String getPrefix(ConversationContext context) {

            return "";

        }
    }

    private class MenuPrompt extends FixedSetPrompt {

        public MenuPrompt() {

            super("1", "2", "3", "4");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            final String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorTitle") + " -\n" + ColorUtil.BLUE + ""
                    + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - " + Lang.get("eventEditorCreate")
                    + "\n" + ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("eventEditorEdit") + "\n" + ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET
                    + ColorUtil.YELLOW + " - " + Lang.get("eventEditorDelete") + "\n" + ColorUtil.GREEN + ""
                    + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - " + Lang.get("exit");

            return text;

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase("1")) {

                if (player.hasPermission("quests.editor.events.create")) {
                    context.setSessionData(CK.E_OLD_EVENT, "");
                    return new EventNamePrompt();
                } else {
                    player.sendMessage(ColorUtil.RED + "You do not have permission to create new Events.");
                    return new MenuPrompt();
                }

            } else if (input.equalsIgnoreCase("2")) {

                if (player.hasPermission("quests.editor.events.edit")) {

                    if (quests.events.isEmpty()) {
                        ((Player) context.getForWhom()).sendMessage(ColorUtil.YELLOW
                                + Lang.get("eventEditorNoneToEdit"));
                        return new MenuPrompt();
                    } else {
                        return new SelectEditPrompt();
                    }

                } else {

                    player.sendMessage(ColorUtil.RED + "You do not have permission to edit Events.");
                    return new MenuPrompt();

                }
            } else if (input.equalsIgnoreCase("3")) {

                if (player.hasPermission("quests.editor.events.delete")) {

                    if (quests.events.isEmpty()) {
                        ((Player) context.getForWhom()).sendMessage(ColorUtil.YELLOW
                                + Lang.get("eventEditorNoneToDelete"));
                        return new MenuPrompt();
                    } else {
                        return new SelectDeletePrompt();
                    }

                } else {

                    player.sendMessage(ColorUtil.RED + "You do not have permission to delete Events.");
                    return new MenuPrompt();

                }

            } else if (input.equalsIgnoreCase("4")) {
                ((Player) context.getForWhom()).sendMessage(ColorUtil.YELLOW + Lang.get("exited"));
                return Prompt.END_OF_CONVERSATION;
            }

            return null;

        }
    }

    public Prompt returnToMenu() {

        return new CreateMenuPrompt();

    }

    public static void clearData(ConversationContext context) {

        context.setSessionData(CK.E_OLD_EVENT, null);
        context.setSessionData(CK.E_NAME, null);
        context.setSessionData(CK.E_MESSAGE, null);
        context.setSessionData(CK.E_CLEAR_INVENTORY, null);
        context.setSessionData(CK.E_FAIL_QUEST, null);
        context.setSessionData(CK.E_ITEMS, null);
        context.setSessionData(CK.E_ITEMS_AMOUNTS, null);
        context.setSessionData(CK.E_EXPLOSIONS, null);
        context.setSessionData(CK.E_EFFECTS, null);
        context.setSessionData(CK.E_EFFECTS_LOCATIONS, null);
        context.setSessionData(CK.E_WORLD_STORM, null);
        context.setSessionData(CK.E_WORLD_STORM_DURATION, null);
        context.setSessionData(CK.E_WORLD_THUNDER, null);
        context.setSessionData(CK.E_WORLD_THUNDER_DURATION, null);
        context.setSessionData(CK.E_MOB_TYPES, null);
        context.setSessionData(CK.E_LIGHTNING, null);
        context.setSessionData(CK.E_POTION_TYPES, null);
        context.setSessionData(CK.E_POTION_DURATIONS, null);
        context.setSessionData(CK.E_POTION_STRENGHT, null);
        context.setSessionData(CK.E_HUNGER, null);
        context.setSessionData(CK.E_SATURATION, null);
        context.setSessionData(CK.E_HEALTH, null);
        context.setSessionData(CK.E_TELEPORT, null);
        context.setSessionData(CK.E_COMMANDS, null);

    }

    public static void loadData(Event event, ConversationContext context) {

        if (event.message != null) {
            context.setSessionData(CK.E_MESSAGE, event.message);
        }

        if (event.clearInv == true) {
            context.setSessionData(CK.E_CLEAR_INVENTORY, "Yes");
        } else {
            context.setSessionData(CK.E_CLEAR_INVENTORY, "No");
        }

        if (event.failQuest == true) {
            context.setSessionData(CK.E_FAIL_QUEST, "Yes");
        } else {
            context.setSessionData(CK.E_FAIL_QUEST, "No");
        }

        if (event.items != null && event.items.isEmpty() == false) {

            final LinkedList<ItemStack> items = new LinkedList<ItemStack>();
            items.addAll(event.items);

            context.setSessionData(CK.E_ITEMS, items);

        }

        if (event.explosions != null && event.explosions.isEmpty() == false) {

            final LinkedList<String> locs = new LinkedList<String>();

            for (final Location loc: event.explosions) {
                locs.add(Quests.getLocationInfo(loc));
            }

            context.setSessionData(CK.E_EXPLOSIONS, locs);

        }

        if (event.effects != null && event.effects.isEmpty() == false) {

            final LinkedList<String> effs = new LinkedList<String>();
            final LinkedList<String> locs = new LinkedList<String>();

            for (final Entry<?, ?> e: event.effects.entrySet()) {

                effs.add(((Effect) e.getKey()).toString());
                locs.add(Quests.getLocationInfo((Location) e.getValue()));

            }

            context.setSessionData(CK.E_EFFECTS, effs);
            context.setSessionData(CK.E_EFFECTS_LOCATIONS, locs);

        }

        if (event.stormWorld != null) {

            context.setSessionData(CK.E_WORLD_STORM, event.stormWorld.getName());
            context.setSessionData(CK.E_WORLD_STORM_DURATION, (long) event.stormDuration);

        }

        if (event.thunderWorld != null) {

            context.setSessionData(CK.E_WORLD_THUNDER, event.thunderWorld.getName());
            context.setSessionData(CK.E_WORLD_THUNDER_DURATION, (long) event.thunderDuration);

        }

        if (event.mobSpawns != null && event.mobSpawns.isEmpty() == false) {

            final LinkedList<String> questMobs = new LinkedList<String>();

            for (final QuestMob questMob: event.mobSpawns) {
                questMobs.add(questMob.serialize());
            }

            context.setSessionData(CK.E_MOB_TYPES, questMobs);
        }

        if (event.lightningStrikes != null && event.lightningStrikes.isEmpty() == false) {

            final LinkedList<String> locs = new LinkedList<String>();
            for (final Location loc: event.lightningStrikes) {
                locs.add(Quests.getLocationInfo(loc));
            }
            context.setSessionData(CK.E_LIGHTNING, locs);

        }

        if (event.potionEffects != null && event.potionEffects.isEmpty() == false) {

            final LinkedList<String> types = new LinkedList<String>();
            final LinkedList<Long> durations = new LinkedList<Long>();
            final LinkedList<Integer> mags = new LinkedList<Integer>();

            for (final PotionEffect pe: event.potionEffects) {

                types.add(pe.getType().getName());
                durations.add((long) pe.getDuration());
                mags.add(pe.getAmplifier());

            }

            context.setSessionData(CK.E_POTION_TYPES, types);
            context.setSessionData(CK.E_POTION_DURATIONS, durations);
            context.setSessionData(CK.E_POTION_STRENGHT, mags);

        }

        if (event.hunger > -1) {

            context.setSessionData(CK.E_HUNGER, event.hunger);

        }

        if (event.saturation > -1) {

            context.setSessionData(CK.E_SATURATION, event.saturation);

        }

        if (event.health > -1) {

            context.setSessionData(CK.E_HEALTH, event.health);

        }

        if (event.teleport != null) {

            context.setSessionData(CK.E_TELEPORT, Quests.getLocationInfo(event.teleport));

        }

        if (event.commands != null) {

            context.setSessionData(CK.E_COMMANDS, event.commands);

        }

    }

    private class SelectEditPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorEdit") + " -\n";

            for (final Event evt: quests.events) {
                text += ColorUtil.AQUA + evt.name + ColorUtil.YELLOW + ", ";
            }

            text = text.substring(0, text.length() - 2) + "\n";
            text += ColorUtil.YELLOW + Lang.get("eventEditorEnterEventName");

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                for (final Event evt: quests.events) {

                    if (evt.name.equalsIgnoreCase(input)) {
                        context.setSessionData(CK.E_OLD_EVENT, evt.name);
                        context.setSessionData(CK.E_NAME, evt.name);
                        EventFactory.loadData(evt, context);
                        return new CreateMenuPrompt();
                    }

                }

                ((Player) context.getForWhom()).sendMessage(ColorUtil.RED + Lang.get("eventEditorNotFound"));
                return new SelectEditPrompt();

            } else {
                return new MenuPrompt();
            }

        }

    }

    private class SelectDeletePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorDelete") + " -\n";

            for (final Event evt: quests.events) {
                text += ColorUtil.AQUA + evt.name + ColorUtil.YELLOW + ",";
            }

            text = text.substring(0, text.length() - 1) + "\n";
            text += ColorUtil.YELLOW + Lang.get("eventEditorEnterEventName");

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                final LinkedList<String> used = new LinkedList<String>();

                for (final Event evt: quests.events) {

                    if (evt.name.equalsIgnoreCase(input)) {

                        for (final Quest quest: quests.getQuests()) {

                            for (final Stage stage: quest.orderedStages) {

                                if (stage.finishEvent != null && stage.finishEvent.name.equalsIgnoreCase(evt.name)) {
                                    used.add(quest.name);
                                    break;
                                }

                            }

                        }

                        if (used.isEmpty()) {
                            context.setSessionData(CK.ED_EVENT_DELETE, evt.name);
                            return new DeletePrompt();
                        } else {
                            ((Player) context.getForWhom()).sendMessage(ColorUtil.RED
                                    + Lang.get("eventEditorEventInUse") + " \"" + ColorUtil.PURPLE + evt.name
                                    + ColorUtil.RED + "\":");
                            for (final String s: used) {
                                ((Player) context.getForWhom()).sendMessage(ColorUtil.RED + "- " + ColorUtil.DARKRED
                                        + s);
                            }
                            ((Player) context.getForWhom()).sendMessage(ColorUtil.RED
                                    + Lang.get("eventEditorMustModifyQuests"));
                            return new SelectDeletePrompt();
                        }
                    }

                }

                ((Player) context.getForWhom()).sendMessage(ColorUtil.RED + Lang.get("eventEditorNotFound"));
                return new SelectDeletePrompt();

            } else {
                return new MenuPrompt();
            }

        }

    }

    private class DeletePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.RED + Lang.get("eventEditorDeletePrompt") + " \"" + ColorUtil.GOLD
                    + (String) context.getSessionData(CK.ED_EVENT_DELETE) + ColorUtil.RED + "\"?\n";
            text += ColorUtil.YELLOW + Lang.get("yes") + "/" + Lang.get("no");

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("yes"))) {
                deleteEvent(context);
                return new MenuPrompt();
            } else if (input.equalsIgnoreCase(Lang.get("no"))) {
                return new MenuPrompt();
            } else {
                return new DeletePrompt();
            }

        }

    }

    private class CreateMenuPrompt extends FixedSetPrompt {

        public CreateMenuPrompt() {

            super("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18",
                    "19");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("event") + ": " + ColorUtil.AQUA
                    + context.getSessionData(CK.E_NAME) + ColorUtil.GOLD + " -\n";

            text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("eventEditorSetName") + "\n";

            if (context.getSessionData(CK.E_MESSAGE) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetMessage") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetMessage") + "(" + ColorUtil.AQUA + "\""
                        + context.getSessionData(CK.E_MESSAGE) + "\"" + ColorUtil.YELLOW + ")\n";
            }

            if (context.getSessionData(CK.E_CLEAR_INVENTORY) == null) {
                context.setSessionData(CK.E_CLEAR_INVENTORY, "No");
            }

            if (context.getSessionData(CK.E_FAIL_QUEST) == null) {
                context.setSessionData(CK.E_FAIL_QUEST, "No");
            }

            text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("eventEditorClearInv") + ": " + ColorUtil.AQUA
                    + context.getSessionData(CK.E_CLEAR_INVENTORY) + "\n";
            text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("eventEditorFailQuest") + ": " + ColorUtil.AQUA
                    + context.getSessionData(CK.E_FAIL_QUEST) + "\n";

            if (context.getSessionData(CK.E_ITEMS) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "5" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetItems") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "5" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetItems") + "\n";
                final LinkedList<ItemStack> items = (LinkedList<ItemStack>) context.getSessionData(CK.E_ITEMS);

                for (final ItemStack is: items) {

                    text += ColorUtil.GRAY + "    - " + ItemUtil.getString(is) + "\n";

                }

            }

            if (context.getSessionData(CK.E_EXPLOSIONS) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "6" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetExplosions") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "6" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetExplosions") + "\n";
                final LinkedList<String> locations = (LinkedList<String>) context.getSessionData(CK.E_EXPLOSIONS);

                for (final String loc: locations) {

                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + loc + "\n";

                }

            }

            if (context.getSessionData(CK.E_EFFECTS) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "7" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetEffects") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "7" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetEffects") + "\n";
                final LinkedList<String> effects = (LinkedList<String>) context.getSessionData(CK.E_EFFECTS);
                final LinkedList<String> locations = (LinkedList<String>) context
                        .getSessionData(CK.E_EFFECTS_LOCATIONS);

                for (final String effect: effects) {

                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + effect + ColorUtil.GRAY + " at "
                            + ColorUtil.DARKAQUA + locations.get(effects.indexOf(effect)) + "\n";

                }

            }

            if (context.getSessionData(CK.E_WORLD_STORM) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "8" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetStorm") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "8" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetStorm") + " (" + ColorUtil.AQUA
                        + (String) context.getSessionData(CK.E_WORLD_STORM) + ColorUtil.YELLOW + " -> "
                        + ColorUtil.DARKAQUA + Quests.getTime((Long) context.getSessionData(CK.E_WORLD_STORM_DURATION))
                        + ColorUtil.YELLOW + ")\n";
            }

            if (context.getSessionData(CK.E_WORLD_THUNDER) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "9" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetThunder") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "9" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetThunder") + " (" + ColorUtil.AQUA
                        + (String) context.getSessionData(CK.E_WORLD_THUNDER) + ColorUtil.YELLOW + " -> "
                        + ColorUtil.DARKAQUA
                        + Quests.getTime((Long) context.getSessionData(CK.E_WORLD_THUNDER_DURATION)) + ColorUtil.YELLOW
                        + ")\n";
            }

            if (context.getSessionData(CK.E_MOB_TYPES) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "10" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetMobSpawns") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {
                final LinkedList<String> types = (LinkedList<String>) context.getSessionData(CK.E_MOB_TYPES);

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "10" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetMobSpawns") + "\n";

                for (final String s: types) {
                    final QuestMob qm = QuestMob.fromString(s);
                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + qm.getType().getName()
                            + ((qm.getName() != null) ? ": " + qm.getName() : "") + ColorUtil.GRAY + " x "
                            + ColorUtil.DARKAQUA + qm.getSpawnAmounts() + ColorUtil.GRAY + " -> " + ColorUtil.GREEN
                            + Quests.getLocationInfo(qm.getSpawnLocation()) + "\n";
                }
            }

            if (context.getSessionData(CK.E_LIGHTNING) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "11" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetLightning") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "11" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetLightning") + "\n";
                final LinkedList<String> locations = (LinkedList<String>) context.getSessionData(CK.E_LIGHTNING);

                for (final String loc: locations) {

                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + loc + "\n";

                }

            }

            if (context.getSessionData(CK.E_POTION_TYPES) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "12" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetPotionEffects") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "12" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetPotionEffects") + "\n";
                final LinkedList<String> types = (LinkedList<String>) context.getSessionData(CK.E_POTION_TYPES);
                final LinkedList<Long> durations = (LinkedList<Long>) context.getSessionData(CK.E_POTION_DURATIONS);
                final LinkedList<Integer> mags = (LinkedList<Integer>) context.getSessionData(CK.E_POTION_STRENGHT);
                int index = -1;

                for (final String type: types) {

                    index++;
                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + type + ColorUtil.PURPLE + " "
                            + Quests.getNumeral(mags.get(index)) + ColorUtil.GRAY + " -> " + ColorUtil.DARKAQUA
                            + Quests.getTime(durations.get(index) * 50L) + "\n";

                }

            }

            if (context.getSessionData(CK.E_HUNGER) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "13" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetHunger") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "13" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetHunger") + ColorUtil.AQUA + " ("
                        + context.getSessionData(CK.E_HUNGER) + ")\n";

            }

            if (context.getSessionData(CK.E_SATURATION) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "14" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetSaturation") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "14" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetSaturation") + ColorUtil.AQUA + " ("
                        + context.getSessionData(CK.E_SATURATION) + ")\n";

            }

            if (context.getSessionData(CK.E_HEALTH) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "15" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetHealth") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "15" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetHealth") + ColorUtil.AQUA + " ("
                        + context.getSessionData(CK.E_HEALTH) + ")\n";

            }

            if (context.getSessionData(CK.E_TELEPORT) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "16" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetTeleport") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "16" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetTeleport") + ColorUtil.AQUA + " ("
                        + (String) context.getSessionData(CK.E_TELEPORT) + ")\n";

            }

            if (context.getSessionData(CK.E_COMMANDS) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "17" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetCommands") + ColorUtil.GRAY + " (" + Lang.get("noneSet") + ")\n";
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "17" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetCommands") + "\n";
                for (final String s: (LinkedList<String>) context.getSessionData(CK.E_COMMANDS)) {
                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + s + "\n";
                }

            }

            text += ColorUtil.GREEN + "" + ColorUtil.BOLD + "18" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("done") + "\n";
            text += ColorUtil.RED + "" + ColorUtil.BOLD + "19" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("quit");

            return text;

        }

        @Override
        public Prompt acceptValidatedInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {

                return new SetNamePrompt();

            } else if (input.equalsIgnoreCase("2")) {

                return new MessagePrompt();

            } else if (input.equalsIgnoreCase("3")) {

                final String s = (String) context.getSessionData(CK.E_CLEAR_INVENTORY);
                if (s.equalsIgnoreCase("Yes")) {
                    context.setSessionData(CK.E_CLEAR_INVENTORY, "No");
                } else {
                    context.setSessionData(CK.E_CLEAR_INVENTORY, "Yes");
                }

                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase("4")) {

                final String s = (String) context.getSessionData(CK.E_FAIL_QUEST);
                if (s.equalsIgnoreCase("Yes")) {
                    context.setSessionData(CK.E_FAIL_QUEST, "No");
                } else {
                    context.setSessionData(CK.E_FAIL_QUEST, "Yes");
                }

                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase("5")) {

                return new ItemListPrompt();

            } else if (input.equalsIgnoreCase("6")) {

                selectedExplosionLocations.put((Player) context.getForWhom(), null);
                return new ExplosionPrompt();

            } else if (input.equalsIgnoreCase("7")) {

                return new EffectListPrompt();

            } else if (input.equalsIgnoreCase("8")) {

                return new StormPrompt();

            } else if (input.equalsIgnoreCase("9")) {

                return new ThunderPrompt();

            } else if (input.equalsIgnoreCase("10")) {

                return new MobPrompt();

            } else if (input.equalsIgnoreCase("11")) {

                selectedLightningLocations.put((Player) context.getForWhom(), null);
                return new LightningPrompt();

            } else if (input.equalsIgnoreCase("12")) {

                return new PotionEffectPrompt();

            } else if (input.equalsIgnoreCase("13")) {

                return new HungerPrompt();

            } else if (input.equalsIgnoreCase("14")) {

                return new SaturationPrompt();

            } else if (input.equalsIgnoreCase("15")) {

                return new HealthPrompt();

            } else if (input.equalsIgnoreCase("16")) {

                selectedTeleportLocations.put((Player) context.getForWhom(), null);
                return new TeleportPrompt();

            } else if (input.equalsIgnoreCase("17")) {

                return new CommandsPrompt();

            } else if (input.equalsIgnoreCase("18")) {

                if (context.getSessionData(CK.E_OLD_EVENT) != null) {
                    return new FinishPrompt((String) context.getSessionData(CK.E_OLD_EVENT));
                } else {
                    return new FinishPrompt(null);
                }

            } else if (input.equalsIgnoreCase("19")) {

                return new QuitPrompt();

            }

            return null;

        }
    }

    private class QuitPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GREEN + Lang.get("eventEditorQuitWithoutSaving") + "\n";
            text += ColorUtil.YELLOW + Lang.get("yes") + "/" + Lang.get("no");

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("yes"))) {
                EventFactory.clearData(context);
                return new MenuPrompt();
            } else if (input.equalsIgnoreCase(Lang.get("no"))) {
                return new CreateMenuPrompt();
            } else {
                ((Player) context.getForWhom()).sendMessage(ColorUtil.RED + Lang.get("invalidOption"));
                return new QuitPrompt();
            }

        }

    }

    private class FinishPrompt extends StringPrompt {

        String             modName  = null;
        LinkedList<String> modified = new LinkedList<String>();

        public FinishPrompt(String modifiedName) {

            if (modifiedName != null) {

                modName = modifiedName;
                for (final Quest q: quests.getQuests()) {

                    for (final Stage s: q.orderedStages) {

                        if (s.finishEvent != null && s.finishEvent.name != null) {

                            if (s.finishEvent.name.equalsIgnoreCase(modifiedName)) {
                                modified.add(q.getName());
                                break;
                            }

                        }

                    }

                }

            }

        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.RED + Lang.get("eventEditorFinishAndSave") + " \"" + ColorUtil.GOLD
                    + (String) context.getSessionData(CK.E_NAME) + ColorUtil.RED + "\"?\n";
            if (modified.isEmpty() == false) {
                text += ColorUtil.RED + Lang.get("eventEditorModifiedNote") + "\n";
                for (final String s: modified) {
                    text += ColorUtil.GRAY + "    - " + ColorUtil.DARKRED + s + "\n";
                }
                text += ColorUtil.RED + Lang.get("eventEditorForcedToQuit") + "\n";
            }
            text += ColorUtil.YELLOW + Lang.get("yes") + "/" + Lang.get("no");

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("yes"))) {
                saveEvent(context);
                return new MenuPrompt();
            } else if (input.equalsIgnoreCase(Lang.get("no"))) {
                return new CreateMenuPrompt();
            } else {
                ((Player) context.getForWhom()).sendMessage(ColorUtil.RED + Lang.get("invalidOption"));
                return new FinishPrompt(modName);
            }

        }

    }

    // Convenience methods to reduce typecasting
    private static String getCString(ConversationContext context, String path) {
        return (String) context.getSessionData(path);
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<String> getCStringList(ConversationContext context, String path) {
        return (LinkedList<String>) context.getSessionData(path);
    }

    private static Integer getCInt(ConversationContext context, String path) {
        return (Integer) context.getSessionData(path);
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<Integer> getCIntList(ConversationContext context, String path) {
        return (LinkedList<Integer>) context.getSessionData(path);
    }

    private static Long getCLong(ConversationContext context, String path) {
        return (Long) context.getSessionData(path);
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<Long> getCLongList(ConversationContext context, String path) {
        return (LinkedList<Long>) context.getSessionData(path);
    }

    //

    private void deleteEvent(ConversationContext context) {

        final YamlConfiguration data = new YamlConfiguration();

        try {
            eventsFile = new File(quests.getDataFolder(), "events.yml");
            data.load(eventsFile);
        } catch (final IOException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("eventEditorErrorReadingFile"));
            return;
        } catch (final InvalidConfigurationException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("eventEditorErrorReadingFile"));
            return;
        }

        final String event = (String) context.getSessionData(CK.ED_EVENT_DELETE);
        final ConfigurationSection sec = data.getConfigurationSection("events");
        sec.set(event, null);

        try {
            data.save(eventsFile);
        } catch (final IOException e) {
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("eventEditorErrorSaving"));
            return;
        }

        quests.reloadQuests();

        ((Player) context.getForWhom()).sendMessage(ChatColor.YELLOW + Lang.get("eventEditorDeleted"));

        for (final Quester q: quests.questers.values()) {
            q.checkQuest();
        }

        EventFactory.clearData(context);

    }

    private void saveEvent(ConversationContext context) {

        final YamlConfiguration data = new YamlConfiguration();

        try {
            eventsFile = new File(quests.getDataFolder(), "events.yml");
            data.load(eventsFile);
        } catch (final IOException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("eventEditorErrorReadingFile"));
            return;
        } catch (final InvalidConfigurationException e) {
            e.printStackTrace();
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("eventEditorErrorReadingFile"));
            return;
        }

        if (((String) context.getSessionData(CK.E_OLD_EVENT)).isEmpty() == false) {
            data.set("events." + (String) context.getSessionData(CK.E_OLD_EVENT), null);
            quests.events.remove(quests.getEvent((String) context.getSessionData(CK.E_OLD_EVENT)));
        }

        final ConfigurationSection section = data.createSection("events." + (String) context.getSessionData(CK.E_NAME));
        names.remove(context.getSessionData(CK.E_NAME));

        if (context.getSessionData(CK.E_MESSAGE) != null) {
            section.set("message", EventFactory.getCString(context, CK.E_MESSAGE));
        }

        if (context.getSessionData(CK.E_CLEAR_INVENTORY) != null) {
            final String s = EventFactory.getCString(context, CK.E_CLEAR_INVENTORY);
            if (s.equalsIgnoreCase("Yes")) {
                section.set("clear-inventory", true);
            }
        }

        if (context.getSessionData(CK.E_FAIL_QUEST) != null) {
            final String s = EventFactory.getCString(context, CK.E_FAIL_QUEST);
            if (s.equalsIgnoreCase("Yes")) {
                section.set("fail-quest", true);
            }
        }

        if (context.getSessionData(CK.E_ITEMS) != null) {

            final LinkedList<ItemStack> items = (LinkedList<ItemStack>) context.getSessionData(CK.E_ITEMS);
            final LinkedList<String> lines = new LinkedList<String>();

            for (final ItemStack is: items) {
                lines.add(ItemUtil.serialize(is));
            }

            section.set("items", lines);

        }

        if (context.getSessionData(CK.E_EXPLOSIONS) != null) {

            final LinkedList<String> locations = EventFactory.getCStringList(context, CK.E_EXPLOSIONS);
            section.set("explosions", locations);

        }

        if (context.getSessionData(CK.E_EFFECTS) != null) {

            final LinkedList<String> effects = EventFactory.getCStringList(context, CK.E_EFFECTS);
            final LinkedList<String> locations = EventFactory.getCStringList(context, CK.E_EFFECTS_LOCATIONS);

            section.set("effects", effects);
            section.set("effect-locations", locations);

        }

        if (context.getSessionData(CK.E_WORLD_STORM) != null) {

            final String world = EventFactory.getCString(context, CK.E_WORLD_STORM);
            final Long duration = EventFactory.getCLong(context, CK.E_WORLD_STORM_DURATION);

            section.set("storm-world", world);
            section.set("storm-duration", duration / 50L);

        }

        if (context.getSessionData(CK.E_WORLD_THUNDER) != null) {

            final String world = EventFactory.getCString(context, CK.E_WORLD_THUNDER);
            final Long duration = EventFactory.getCLong(context, CK.E_WORLD_THUNDER_DURATION);

            section.set("thunder-world", world);
            section.set("thunder-duration", duration / 50L);

        }

        try {
            if (context.getSessionData(CK.E_MOB_TYPES) != null) {
                int count = 0;

                for (final String s: EventFactory.getCStringList(context, CK.E_MOB_TYPES)) {
                    ConfigurationSection ss = section.getConfigurationSection("mob-spawns." + count);
                    if (ss == null) {
                        ss = section.createSection("mob-spawns." + count);
                    }
                    final QuestMob questMob = QuestMob.fromString(s);

                    if (questMob == null) {
                        continue;
                    }

                    ss.set("name", questMob.getName());
                    ss.set("spawn-location", Quests.getLocationInfo(questMob.getSpawnLocation()));
                    ss.set("mob-type", questMob.getType().getName());
                    ss.set("spawn-amounts", questMob.getSpawnAmounts());
                    ss.set("held-item", ItemUtil.serialize(questMob.inventory[0]));
                    ss.set("held-item-drop-chance", questMob.dropChances[0]);
                    ss.set("boots", ItemUtil.serialize(questMob.inventory[1]));
                    ss.set("boots-drop-chance", questMob.dropChances[1]);
                    ss.set("leggings", ItemUtil.serialize(questMob.inventory[2]));
                    ss.set("leggings-drop-chance", questMob.dropChances[2]);
                    ss.set("chest-plate", ItemUtil.serialize(questMob.inventory[3]));
                    ss.set("chest-plate-drop-chance", questMob.dropChances[3]);
                    ss.set("helmet", ItemUtil.serialize(questMob.inventory[4]));
                    ss.set("helmet-drop-chance", questMob.dropChances[4]);
                    count++;
                }

            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (context.getSessionData(CK.E_LIGHTNING) != null) {

            final LinkedList<String> locations = EventFactory.getCStringList(context, CK.E_LIGHTNING);
            section.set("lightning-strikes", locations);

        }

        if (context.getSessionData(CK.E_COMMANDS) != null) {

            final LinkedList<String> commands = EventFactory.getCStringList(context, CK.E_COMMANDS);
            if (commands.isEmpty() == false) {
                section.set("commands", commands);
            }

        }

        if (context.getSessionData(CK.E_POTION_TYPES) != null) {

            final LinkedList<String> types = EventFactory.getCStringList(context, CK.E_POTION_TYPES);
            final LinkedList<Long> durations = EventFactory.getCLongList(context, CK.E_POTION_DURATIONS);
            final LinkedList<Integer> mags = EventFactory.getCIntList(context, CK.E_POTION_STRENGHT);

            section.set("potion-effect-types", types);
            section.set("potion-effect-durations", durations);
            section.set("potion-effect-amplifiers", mags);

        }

        if (context.getSessionData(CK.E_HUNGER) != null) {

            final Integer i = EventFactory.getCInt(context, CK.E_HUNGER);
            section.set("hunger", i);

        }

        if (context.getSessionData(CK.E_SATURATION) != null) {

            final Integer i = EventFactory.getCInt(context, CK.E_SATURATION);
            section.set("saturation", i);

        }

        if (context.getSessionData(CK.E_HEALTH) != null) {

            final Integer i = EventFactory.getCInt(context, CK.E_HEALTH);
            section.set("health", i);

        }

        if (context.getSessionData(CK.E_TELEPORT) != null) {

            section.set("teleport-location", EventFactory.getCString(context, CK.E_TELEPORT));

        }

        try {
            data.save(eventsFile);
        } catch (final IOException e) {
            ((Player) context.getForWhom()).sendMessage(ChatColor.RED + Lang.get("eventEditorErrorSaving"));
            return;
        }

        quests.reloadQuests();

        ((Player) context.getForWhom()).sendMessage(ChatColor.YELLOW + Lang.get("eventEditorSaved"));

        for (final Quester q: quests.questers.values()) {
            q.checkQuest();
        }

        EventFactory.clearData(context);

    }

    private class EventNamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            final String text = ColorUtil.AQUA + Lang.get("eventEditorCreate") + ColorUtil.GOLD + " - "
                    + Lang.get("eventEditorEnterEventName");

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                for (final Event e: quests.events) {

                    if (e.name.equalsIgnoreCase(input)) {

                        context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorExists"));
                        return new EventNamePrompt();

                    }

                }

                if (names.contains(input)) {

                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorSomeone"));
                    return new EventNamePrompt();

                }

                if (StringUtils.isAlphanumeric(input) == false) {

                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorAlpha"));
                    return new EventNamePrompt();

                }

                context.setSessionData(CK.E_NAME, input);
                names.add(input);
                return new CreateMenuPrompt();

            } else {

                return new MenuPrompt();

            }

        }
    }

    private class ExplosionPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorExplosionPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdAdd"))) {

                final Block block = selectedExplosionLocations.get(player);
                if (block != null) {

                    final Location loc = block.getLocation();

                    LinkedList<String> locs;
                    if (context.getSessionData(CK.E_EXPLOSIONS) != null) {
                        locs = (LinkedList<String>) context.getSessionData(CK.E_EXPLOSIONS);
                    } else {
                        locs = new LinkedList<String>();
                    }

                    locs.add(Quests.getLocationInfo(loc));
                    context.setSessionData(CK.E_EXPLOSIONS, locs);
                    selectedExplosionLocations.remove(player);

                } else {
                    player.sendMessage(ColorUtil.RED + Lang.get("eventEditorSelectBlockFirst"));
                    return new ExplosionPrompt();
                }

                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdClear"))) {

                context.setSessionData(CK.E_EXPLOSIONS, null);
                selectedExplosionLocations.remove(player);
                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {

                selectedExplosionLocations.remove(player);
                return new CreateMenuPrompt();

            } else {
                return new ExplosionPrompt();
            }

        }
    }

    private class SetNamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorEnterEventName");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                for (final Event e: quests.events) {

                    if (e.name.equalsIgnoreCase(input)) {
                        context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorExists"));
                        return new SetNamePrompt();
                    }

                }

                if (names.contains(input)) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorSomeone"));
                    return new SetNamePrompt();
                }

                if (StringUtils.isAlphanumeric(input) == false) {

                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorAlpha"));
                    return new SetNamePrompt();

                }

                names.remove(context.getSessionData(CK.E_NAME));
                context.setSessionData(CK.E_NAME, input);
                names.add(input);

            }

            return new CreateMenuPrompt();

        }
    }

    private class MessagePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetMessagePrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false && input.equalsIgnoreCase("cmdNone") == false) {
                context.setSessionData(CK.E_MESSAGE, input);
            } else if (input.equalsIgnoreCase(Lang.get("cmdNone"))) {
                context.setSessionData(CK.E_MESSAGE, null);
            }

            return new CreateMenuPrompt();

        }
    }

    private class ItemListPrompt extends FixedSetPrompt {

        public ItemListPrompt() {

            super("1", "2", "3");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            // Check/add newly made item
            if (context.getSessionData("newItem") != null) {
                if (context.getSessionData(CK.E_ITEMS) != null) {
                    final List<ItemStack> items = getItems(context);
                    items.add((ItemStack) context.getSessionData("tempStack"));
                    context.setSessionData(CK.E_ITEMS, items);
                } else {
                    final LinkedList<ItemStack> itemRews = new LinkedList<ItemStack>();
                    itemRews.add((ItemStack) context.getSessionData("tempStack"));
                    context.setSessionData(CK.E_ITEMS, itemRews);
                }

                context.setSessionData("newItem", null);
                context.setSessionData("tempStack", null);

            }

            String text = ColorUtil.GOLD + "- Give Items -\n";
            if (context.getSessionData(CK.E_ITEMS) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW
                        + " - Add item\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - Clear\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - Done";
            } else {

                for (final ItemStack is: getItems(context)) {

                    text += ColorUtil.GRAY + "    - " + ItemUtil.getDisplayString(is) + "\n";

                }

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW
                        + " - Add item\n";

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - Clear\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - Done";

            }

            return text;

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {
                return new ItemStackPrompt(ItemListPrompt.this);
            } else if (input.equalsIgnoreCase("2")) {
                context.getForWhom().sendRawMessage(ColorUtil.YELLOW + "Event Items cleared.");
                context.setSessionData(CK.E_ITEMS, null);
                return new ItemListPrompt();
            } else if (input.equalsIgnoreCase("3")) {
                return new CreateMenuPrompt();
            }
            return null;

        }

        private List<ItemStack> getItems(ConversationContext context) {
            return (List<ItemStack>) context.getSessionData(CK.E_ITEMS);
        }

    }

    private class EffectListPrompt extends FixedSetPrompt {

        public EffectListPrompt() {

            super("1", "2", "3", "4");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorEffects") + " -\n";
            if (context.getSessionData(CK.E_EFFECTS) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorAddEffect") + " (" + Lang.get("noneSet") + ")\n";
                text += ColorUtil.GRAY + "2 - Add effect location (" + Lang.get("eventEditorNoEffects") + ")\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorAddEffect") + "\n";
                for (final String s: getEffects(context)) {

                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + s + "\n";

                }

                if (context.getSessionData(CK.E_EFFECTS_LOCATIONS) == null) {
                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorAddEffectLocation") + " (" + Lang.get("noneSet") + ")\n";
                } else {

                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorAddEffectLocation") + "\n";
                    for (final String s: getEffectLocations(context)) {

                        text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + s + "\n";

                    }

                }

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");

            }

            return text;

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {
                return new EffectPrompt();
            } else if (input.equalsIgnoreCase("2")) {
                if (context.getSessionData(CK.E_EFFECTS) == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustAddEffects"));
                    return new EffectListPrompt();
                } else {
                    selectedEffectLocations.put((Player) context.getForWhom(), null);
                    return new EffectLocationPrompt();
                }
            } else if (input.equalsIgnoreCase("3")) {
                context.getForWhom().sendRawMessage(ColorUtil.YELLOW + Lang.get("eventEditorEffectsCleared"));
                context.setSessionData(CK.E_EFFECTS, null);
                context.setSessionData(CK.E_EFFECTS_LOCATIONS, null);
                return new EffectListPrompt();
            } else if (input.equalsIgnoreCase("4")) {

                int one;
                int two;

                if (context.getSessionData(CK.E_EFFECTS) != null) {
                    one = getEffects(context).size();
                } else {
                    one = 0;
                }

                if (context.getSessionData(CK.E_EFFECTS_LOCATIONS) != null) {
                    two = getEffectLocations(context).size();
                } else {
                    two = 0;
                }

                if (one == two) {
                    return new CreateMenuPrompt();
                } else {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorListSizeMismatch"));
                    return new EffectListPrompt();
                }
            }
            return null;

        }

        private List<String> getEffects(ConversationContext context) {
            return (List<String>) context.getSessionData(CK.E_EFFECTS);
        }

        private List<String> getEffectLocations(ConversationContext context) {
            return (List<String>) context.getSessionData(CK.E_EFFECTS_LOCATIONS);
        }
    }

    private class EffectLocationPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorEffectLocationPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdAdd"))) {

                final Block block = selectedEffectLocations.get(player);
                if (block != null) {

                    final Location loc = block.getLocation();

                    LinkedList<String> locs;
                    if (context.getSessionData(CK.E_EFFECTS_LOCATIONS) != null) {
                        locs = (LinkedList<String>) context.getSessionData(CK.E_EFFECTS_LOCATIONS);
                    } else {
                        locs = new LinkedList<String>();
                    }

                    locs.add(Quests.getLocationInfo(loc));
                    context.setSessionData(CK.E_EFFECTS_LOCATIONS, locs);
                    selectedEffectLocations.remove(player);

                } else {
                    player.sendMessage(ColorUtil.RED + Lang.get("eventEditorSelectBlockFirst"));
                    return new EffectLocationPrompt();
                }

                return new EffectListPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {

                selectedEffectLocations.remove(player);
                return new EffectListPrompt();

            } else {
                return new EffectLocationPrompt();
            }

        }
    }

    private class EffectPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String effects = ColorUtil.PINK + "- Effects - \n";
            effects += ColorUtil.PURPLE + "BLAZE_SHOOT " + ColorUtil.GRAY + "- " + Lang.get("effBlazeShoot") + "\n";
            effects += ColorUtil.PURPLE + "BOW_FIRE " + ColorUtil.GRAY + "- " + Lang.get("effBowFire") + "\n";
            effects += ColorUtil.PURPLE + "CLICK1 " + ColorUtil.GRAY + "- " + Lang.get("effClick1") + "\n";
            effects += ColorUtil.PURPLE + "CLICK2 " + ColorUtil.GRAY + "- " + Lang.get("effClick2") + "\n";
            effects += ColorUtil.PURPLE + "DOOR_TOGGLE " + ColorUtil.GRAY + "- " + Lang.get("effDoorToggle") + "\n";
            effects += ColorUtil.PURPLE + "EXTINGUISH " + ColorUtil.GRAY + "- " + Lang.get("effExtinguish") + "\n";
            effects += ColorUtil.PURPLE + "GHAST_SHOOT " + ColorUtil.GRAY + "- " + Lang.get("effGhastShoot") + "\n";
            effects += ColorUtil.PURPLE + "GHAST_SHRIEK " + ColorUtil.GRAY + "- " + Lang.get("effGhastShriek") + "\n";
            effects += ColorUtil.PURPLE + "ZOMBIE_CHEW_IRON_DOOR " + ColorUtil.GRAY + "- " + Lang.get("effZombieWood")
                    + "\n";
            effects += ColorUtil.PURPLE + "ZOMBIE_CHEW_WOODEN_DOOR " + ColorUtil.GRAY + "- "
                    + Lang.get("effZombieIron") + "\n";

            return ColorUtil.YELLOW + effects + Lang.get("effEnterName");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                if (Quests.getEffect(input.toUpperCase()) != null) {

                    LinkedList<String> effects;
                    if (context.getSessionData(CK.E_EFFECTS) != null) {
                        effects = (LinkedList<String>) context.getSessionData(CK.E_EFFECTS);
                    } else {
                        effects = new LinkedList<String>();
                    }

                    effects.add(input.toUpperCase());
                    context.setSessionData(CK.E_EFFECTS, effects);
                    selectedEffectLocations.remove(player);
                    return new EffectListPrompt();

                } else {
                    player.sendMessage(ColorUtil.PINK + input + " " + ColorUtil.RED
                            + Lang.get("eventEditorInvalidEffect"));
                    return new EffectPrompt();
                }

            } else {

                selectedEffectLocations.remove(player);
                return new EffectListPrompt();

            }

        }
    }

    private class StormPrompt extends FixedSetPrompt {

        public StormPrompt() {

            super("1", "2", "3", "4");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorStorm") + " -\n";
            if (context.getSessionData(CK.E_WORLD_STORM) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetWorld") + " (" + Lang.get("noneSet") + ")\n";
                text += ColorUtil.GRAY + "2 - " + Lang.get("eventEditorSetDuration") + " "
                        + Lang.get("eventEditorNoWorld") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetWorld") + " (" + ColorUtil.AQUA
                        + ((String) context.getSessionData(CK.E_WORLD_STORM)) + ColorUtil.YELLOW + ")\n";

                if (context.getSessionData(CK.E_WORLD_STORM_DURATION) == null) {
                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorSetDuration") + " (" + Lang.get("noneSet") + ")\n";
                } else {

                    final Long dur = (Long) context.getSessionData(CK.E_WORLD_STORM_DURATION);

                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorSetDuration") + " (" + ColorUtil.AQUA + Quests.getTime(dur)
                            + ColorUtil.YELLOW + ")\n";

                }

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");

            }

            return text;

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {
                return new StormWorldPrompt();
            } else if (input.equalsIgnoreCase("2")) {
                if (context.getSessionData(CK.E_WORLD_STORM) == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorSetWorldFirst"));
                    return new StormPrompt();
                } else {
                    return new StormDurationPrompt();
                }
            } else if (input.equalsIgnoreCase("3")) {
                context.getForWhom().sendRawMessage(ColorUtil.YELLOW + Lang.get("eventEditorStormCleared"));
                context.setSessionData(CK.E_WORLD_STORM, null);
                context.setSessionData(CK.E_WORLD_STORM_DURATION, null);
                return new StormPrompt();
            } else if (input.equalsIgnoreCase("4")) {

                if (context.getSessionData(CK.E_WORLD_STORM) != null
                        && context.getSessionData(CK.E_WORLD_STORM_DURATION) == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustSetStormDuration"));
                    return new StormPrompt();
                } else {
                    return new CreateMenuPrompt();
                }

            }
            return null;

        }
    }

    private class StormWorldPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String effects = ColorUtil.PINK + "- " + Lang.get("worlds") + " - \n" + ColorUtil.PURPLE;
            for (final World w: quests.getServer().getWorlds()) {
                effects += w.getName() + ", ";
            }

            effects = effects.substring(0, effects.length());

            return ColorUtil.YELLOW + effects + Lang.get("eventEditorEnterStormWorld");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                if (quests.getServer().getWorld(input) != null) {

                    context.setSessionData(CK.E_WORLD_STORM, quests.getServer().getWorld(input).getName());

                } else {
                    player.sendMessage(ColorUtil.PINK + input + " " + ColorUtil.RED
                            + Lang.get("eventEditorInvalidWorld"));
                    return new StormWorldPrompt();
                }

            }
            return new StormPrompt();

        }
    }

    private class StormDurationPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorEnterStormDuration");

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {

            if (input.longValue() < 1000) {
                context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorAtLeastOneSecond"));
                return new StormDurationPrompt();
            }

            context.setSessionData(CK.E_WORLD_STORM_DURATION, input.longValue());
            return new StormPrompt();

        }
    }

    private class ThunderPrompt extends FixedSetPrompt {

        public ThunderPrompt() {

            super("1", "2", "3", "4");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorThunder") + " -\n";

            if (context.getSessionData(CK.E_WORLD_THUNDER) == null) {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetWorld") + " (" + Lang.get("noneSet") + ")\n";
                text += ColorUtil.GRAY + "2 - " + Lang.get("eventEditorSetDuration") + " "
                        + Lang.get("eventEditorNoWorld") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");

            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetWorld") + " (" + ColorUtil.AQUA
                        + ((String) context.getSessionData(CK.E_WORLD_THUNDER)) + ColorUtil.YELLOW + ")\n";

                if (context.getSessionData(CK.E_WORLD_THUNDER_DURATION) == null) {
                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorSetDuration") + " (" + Lang.get("noneSet") + ")\n";
                } else {

                    final Long dur = (Long) context.getSessionData(CK.E_WORLD_THUNDER_DURATION);
                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorSetDuration") + " (" + ColorUtil.AQUA + Quests.getTime(dur)
                            + ColorUtil.YELLOW + ")\n";

                }

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");

            }

            return text;

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {
                return new ThunderWorldPrompt();
            } else if (input.equalsIgnoreCase("2")) {
                if (context.getSessionData(CK.E_WORLD_THUNDER) == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorSetWorldFirst"));
                    return new ThunderPrompt();
                } else {
                    return new ThunderDurationPrompt();
                }
            } else if (input.equalsIgnoreCase("3")) {
                context.getForWhom().sendRawMessage(ColorUtil.YELLOW + Lang.get("eventEditorThunderCleared"));
                context.setSessionData(CK.E_WORLD_THUNDER, null);
                context.setSessionData(CK.E_WORLD_THUNDER_DURATION, null);
                return new ThunderPrompt();
            } else if (input.equalsIgnoreCase("4")) {

                if (context.getSessionData(CK.E_WORLD_THUNDER) != null
                        && context.getSessionData(CK.E_WORLD_THUNDER_DURATION) == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustSetThunderDuration"));
                    return new ThunderPrompt();
                } else {
                    return new CreateMenuPrompt();
                }

            }
            return null;

        }
    }

    private class ThunderWorldPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String effects = ColorUtil.PINK + "- Worlds - \n" + ColorUtil.PURPLE;
            for (final World w: quests.getServer().getWorlds()) {
                effects += w.getName() + ", ";
            }

            effects = effects.substring(0, effects.length());

            return ColorUtil.YELLOW + effects + Lang.get("eventEditorEnterThunderWorld");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                if (quests.getServer().getWorld(input) != null) {

                    context.setSessionData(CK.E_WORLD_THUNDER, quests.getServer().getWorld(input).getName());

                } else {
                    player.sendMessage(ColorUtil.PINK + input + " " + ColorUtil.RED
                            + Lang.get("eventEditorInvalidWorld"));
                    return new ThunderWorldPrompt();
                }

            }
            return new ThunderPrompt();

        }
    }

    private class ThunderDurationPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorEnterDuration");

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {

            if (input.longValue() < 1000) {
                context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorAtLeastOneSecond"));
                return new ThunderDurationPrompt();
            } else {
                context.setSessionData(CK.E_WORLD_THUNDER_DURATION, input.longValue());
            }

            return new ThunderPrompt();

        }
    }

    private class MobPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorMobSpawns") + " -\n";
            if (context.getSessionData(CK.E_MOB_TYPES) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorAddMobTypes") + " (" + Lang.get("noneSet") + ")\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");
            } else {
                final LinkedList<String> types = (LinkedList<String>) context.getSessionData(CK.E_MOB_TYPES);

                for (int i = 0; i < types.size(); i++) {
                    final QuestMob qm = QuestMob.fromString(types.get(i));
                    text += ColorUtil.GOLD + "  " + (i + 1) + " - Edit: " + ColorUtil.AQUA + qm.getType().getName()
                            + ((qm.getName() != null) ? ": " + qm.getName() : "") + ColorUtil.GRAY + " x "
                            + ColorUtil.DARKAQUA + qm.getSpawnAmounts() + ColorUtil.GRAY + " -> " + ColorUtil.GREEN
                            + Quests.getLocationInfo(qm.getSpawnLocation()) + "\n";
                }

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + (types.size() + 1) + ColorUtil.RESET + ColorUtil.YELLOW
                        + " - " + Lang.get("eventEditorAddMobTypes") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + (types.size() + 2) + ColorUtil.RESET + ColorUtil.YELLOW
                        + " - " + Lang.get("clear") + "\n";
                text += ColorUtil.GREEN + "" + ColorUtil.BOLD + (types.size() + 3) + ColorUtil.RESET + ColorUtil.YELLOW
                        + " - " + Lang.get("done");

            }

            return text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (context.getSessionData(CK.E_MOB_TYPES) == null) {
                if (input.equalsIgnoreCase("1")) {
                    return new QuestMobPrompt(0, null);
                } else if (input.equalsIgnoreCase("2")) {
                    context.getForWhom().sendRawMessage(ColorUtil.YELLOW + Lang.get("eventEditorMobSpawnsCleared"));
                    context.setSessionData(CK.E_MOB_TYPES, null);
                    return new MobPrompt();
                } else if (input.equalsIgnoreCase("3")) {
                    return new CreateMenuPrompt();
                }
            } else {
                final LinkedList<String> types = (LinkedList<String>) context.getSessionData(CK.E_MOB_TYPES);
                int inp;
                try {
                    inp = Integer.parseInt(input);
                } catch (final NumberFormatException e) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorNotANumber"));
                    return new MobPrompt();
                }

                if (inp == types.size() + 1) {
                    return new QuestMobPrompt(inp - 1, null);
                } else if (inp == types.size() + 2) {
                    context.getForWhom().sendRawMessage(ColorUtil.YELLOW + Lang.get("eventEditorMobSpawnsCleared"));
                    context.setSessionData(CK.E_MOB_TYPES, null);
                    return new MobPrompt();
                } else if (inp == types.size() + 3) {
                    return new CreateMenuPrompt();
                } else if (inp > types.size()) {
                    return new MobPrompt();
                } else {
                    return new QuestMobPrompt(inp - 1, QuestMob.fromString(types.get(inp - 1)));
                }
            }

            return new MobPrompt();
        }
    }

    private class QuestMobPrompt extends StringPrompt {

        private QuestMob      questMob;
        private Integer       itemIndex = -1;
        private final Integer mobIndex;

        public QuestMobPrompt(int mobIndex, QuestMob questMob) {
            this.questMob = questMob;
            this.mobIndex = mobIndex;
        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorAddMobTypes") + " - \n";

            if (questMob == null) {
                questMob = new QuestMob();
            }

            // Check/add newly made item

            if (context.getSessionData("newItem") != null) {
                if (itemIndex >= 0) {
                    questMob.inventory[itemIndex] = ((ItemStack) context.getSessionData("tempStack"));
                    itemIndex = -1;
                }

                context.setSessionData("newItem", null);
                context.setSessionData("tempStack", null);

            }

            text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("eventEditorSetMobName") + ColorUtil.GRAY + " ("
                    + ((questMob.getName() == null) ? Lang.get("noneSet") : ColorUtil.AQUA + questMob.getName())
                    + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "2"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobType")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.getType() == null) ? Lang.get("eventEditorNoTypesSet") : ColorUtil.AQUA
                            + questMob.getType().getName()) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "3"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorAddSpawnLocation")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.getSpawnLocation() == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA
                            + Quests.getLocationInfo(questMob.getSpawnLocation())) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "4"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobSpawnAmount")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.getSpawnAmounts() == null) ? ColorUtil.GRAY + Lang.get("eventEditorNoAmountsSet")
                            : ColorUtil.AQUA + "" + questMob.getSpawnAmounts()) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "5"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobItemInHand")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.inventory[0] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA
                            + ItemUtil.getDisplayString(questMob.inventory[0])) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "6"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobItemInHandDrop")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.dropChances[0] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA + ""
                            + questMob.dropChances[0]) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "7"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobBoots")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.inventory[1] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA
                            + ItemUtil.getDisplayString(questMob.inventory[1])) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "8"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobBootsDrop")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.dropChances[1] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA + ""
                            + questMob.dropChances[1]) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "9"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobLeggings")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.inventory[2] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA
                            + ItemUtil.getDisplayString(questMob.inventory[2])) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "10"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobLeggingsDrop")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.dropChances[2] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA + ""
                            + questMob.dropChances[2]) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "11"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobChestPlate")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.inventory[3] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA
                            + ItemUtil.getDisplayString(questMob.inventory[3])) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "12"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobChestPlateDrop")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.dropChances[3] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA + ""
                            + questMob.dropChances[3]) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "13"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobHelmet")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.inventory[4] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA
                            + ItemUtil.getDisplayString(questMob.inventory[4])) + ColorUtil.GRAY + ")\n";
            text += ColorUtil.BLUE
                    + ""
                    + ColorUtil.BOLD
                    + "14"
                    + ColorUtil.RESET
                    + ColorUtil.YELLOW
                    + " - "
                    + Lang.get("eventEditorSetMobHelmetDrop")
                    + ColorUtil.GRAY
                    + " ("
                    + ((questMob.dropChances[4] == null) ? ColorUtil.GRAY + Lang.get("noneSet") : ColorUtil.AQUA + ""
                            + questMob.dropChances[4]) + ColorUtil.GRAY + ")\n";

            text += ColorUtil.GREEN + "" + ColorUtil.BOLD + "15" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("done") + "\n";
            text += ColorUtil.RED + "" + ColorUtil.BOLD + "16" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                    + Lang.get("cancel");

            return text;

        }

        @SuppressWarnings("unchecked")
        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {
                return new MobNamePrompt(mobIndex, questMob);
            } else if (input.equalsIgnoreCase("2")) {
                return new MobTypePrompt(mobIndex, questMob);
            } else if (input.equalsIgnoreCase("3")) {
                selectedMobLocations.put((Player) context.getForWhom(), null);
                return new MobLocationPrompt(mobIndex, questMob);
            } else if (input.equalsIgnoreCase("4")) {
                return new MobAmountPrompt(mobIndex, questMob);
            } else if (input.equalsIgnoreCase("5")) {
                itemIndex = 0;
                return new ItemStackPrompt(QuestMobPrompt.this);
            } else if (input.equalsIgnoreCase("6")) {
                return new MobDropPrompt(0, mobIndex, questMob);
            } else if (input.equalsIgnoreCase("7")) {
                itemIndex = 1;
                return new ItemStackPrompt(QuestMobPrompt.this);
            } else if (input.equalsIgnoreCase("8")) {
                return new MobDropPrompt(1, mobIndex, questMob);
            } else if (input.equalsIgnoreCase("9")) {
                itemIndex = 2;
                return new ItemStackPrompt(QuestMobPrompt.this);
            } else if (input.equalsIgnoreCase("10")) {
                return new MobDropPrompt(2, mobIndex, questMob);
            } else if (input.equalsIgnoreCase("11")) {
                itemIndex = 3;
                return new ItemStackPrompt(QuestMobPrompt.this);
            } else if (input.equalsIgnoreCase("12")) {
                return new MobDropPrompt(3, mobIndex, questMob);
            } else if (input.equalsIgnoreCase("13")) {
                itemIndex = 4;
                return new ItemStackPrompt(QuestMobPrompt.this);
            } else if (input.equalsIgnoreCase("14")) {
                return new MobDropPrompt(4, mobIndex, questMob);
            } else if (input.equalsIgnoreCase("15")) {
                if (questMob.getType() == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustSetMobTypesFirst"));
                    return new QuestMobPrompt(mobIndex, questMob);
                } else if (questMob.getSpawnLocation() == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustSetMobLocationFirst"));
                    return new QuestMobPrompt(mobIndex, questMob);
                } else if (questMob.getSpawnAmounts() == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustSetMobAmountsFirst"));
                    return new QuestMobPrompt(mobIndex, questMob);
                }
                if (context.getSessionData(CK.E_MOB_TYPES) == null) {
                    final LinkedList<String> list = new LinkedList<String>();
                    list.add(questMob.serialize());
                    context.setSessionData(CK.E_MOB_TYPES, list);
                } else {
                    if (((LinkedList<String>) context.getSessionData(CK.E_MOB_TYPES)).isEmpty()) {
                        final LinkedList<String> list = new LinkedList<String>();
                        list.add(questMob.serialize());
                        context.setSessionData(CK.E_MOB_TYPES, list);
                    } else {
                        final LinkedList<String> list = (LinkedList<String>) context.getSessionData(CK.E_MOB_TYPES);
                        list.set(mobIndex, questMob.serialize());
                        context.setSessionData(CK.E_MOB_TYPES, list);
                    }
                }
                return new MobPrompt();
            } else if (input.equalsIgnoreCase("16")) {
                return new MobPrompt();
            } else {
                return new QuestMobPrompt(mobIndex, questMob);
            }

        }
    }

    private class MobNamePrompt extends StringPrompt {

        private final QuestMob questMob;
        private final Integer  mobIndex;

        public MobNamePrompt(int mobIndex, QuestMob questMob) {
            this.questMob = questMob;
            this.mobIndex = mobIndex;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            final String text = ColorUtil.YELLOW + Lang.get("eventEditorSetMobNamePrompt");
            return text;
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {
                return new QuestMobPrompt(mobIndex, questMob);
            } else if (input.equalsIgnoreCase(Lang.get("cmdClear"))) {
                questMob.setName(null);
                return new QuestMobPrompt(mobIndex, questMob);
            } else {
                input = ChatColor.translateAlternateColorCodes('&', input);
                questMob.setName(input);
                return new QuestMobPrompt(mobIndex, questMob);
            }
        }
    }

    private class MobTypePrompt extends StringPrompt {

        private final QuestMob questMob;
        private final Integer  mobIndex;

        public MobTypePrompt(int mobIndex, QuestMob questMob) {
            this.questMob = questMob;
            this.mobIndex = mobIndex;
        }

        @Override
        public String getPromptText(ConversationContext arg0) {
            String mobs = ColorUtil.PINK + "- " + Lang.get("mobs") + " - \n";
            mobs += ColorUtil.PURPLE + "Bat, ";
            mobs += ColorUtil.PURPLE + "Blaze, ";
            mobs += ColorUtil.PURPLE + "CaveSpider, ";
            mobs += ColorUtil.PURPLE + "Chicken, ";
            mobs += ColorUtil.PURPLE + "Cow, ";
            mobs += ColorUtil.PURPLE + "Creeper, ";
            mobs += ColorUtil.PURPLE + "Enderman, ";
            mobs += ColorUtil.PURPLE + "EnderDragon, ";
            mobs += ColorUtil.PURPLE + "Ghast, ";
            mobs += ColorUtil.PURPLE + "Giant, ";
            mobs += ColorUtil.PURPLE + "Horse, ";
            mobs += ColorUtil.PURPLE + "IronGolem, ";
            mobs += ColorUtil.PURPLE + "MagmaCube, ";
            mobs += ColorUtil.PURPLE + "MushroomCow, ";
            mobs += ColorUtil.PURPLE + "Ocelot, ";
            mobs += ColorUtil.PURPLE + "Pig, ";
            mobs += ColorUtil.PURPLE + "PigZombie, ";
            mobs += ColorUtil.PURPLE + "Sheep, ";
            mobs += ColorUtil.PURPLE + "Silverfish, ";
            mobs += ColorUtil.PURPLE + "Skeleton, ";
            mobs += ColorUtil.PURPLE + "Slime, ";
            mobs += ColorUtil.PURPLE + "Snowman, ";
            mobs += ColorUtil.PURPLE + "Spider, ";
            mobs += ColorUtil.PURPLE + "Squid, ";
            mobs += ColorUtil.PURPLE + "Villager, ";
            mobs += ColorUtil.PURPLE + "Witch, ";
            mobs += ColorUtil.PURPLE + "Wither, ";
            mobs += ColorUtil.PURPLE + "Wolf, ";
            mobs += ColorUtil.PURPLE + "Zombie\n";

            return mobs + ColorUtil.YELLOW + Lang.get("eventEditorSetMobTypesPrompt");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                if (Quests.getMobType(input) != null) {

                    questMob.setType(Quests.getMobType(input));

                } else {
                    player.sendMessage(ColorUtil.PINK + input + " " + ColorUtil.RED + Lang.get("eventEditorInvalidMob"));
                    return new MobTypePrompt(mobIndex, questMob);
                }
            }

            return new QuestMobPrompt(mobIndex, questMob);
        }
    }

    private class MobAmountPrompt extends StringPrompt {

        private final QuestMob questMob;
        private final Integer  mobIndex;

        public MobAmountPrompt(int mobIndex, QuestMob questMob) {
            this.questMob = questMob;
            this.mobIndex = mobIndex;
        }

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetMobAmountsPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                try {

                    final int i = Integer.parseInt(input);

                    if (i < 1) {
                        player.sendMessage(ColorUtil.PINK + input + " " + ColorUtil.RED
                                + Lang.get("eventEditorNotGreaterThanZero"));
                        return new MobAmountPrompt(mobIndex, questMob);
                    }

                    questMob.setSpawnAmounts(i);
                    return new QuestMobPrompt(mobIndex, questMob);
                } catch (final NumberFormatException e) {
                    player.sendMessage(ColorUtil.PINK + input + " " + ColorUtil.RED + Lang.get("eventEditorNotANumber"));
                    return new MobAmountPrompt(mobIndex, questMob);
                }

            }

            return new QuestMobPrompt(mobIndex, questMob);

        }
    }

    private class MobLocationPrompt extends StringPrompt {

        private final QuestMob questMob;
        private final Integer  mobIndex;

        public MobLocationPrompt(int mobIndex, QuestMob questMob) {
            this.questMob = questMob;
            this.mobIndex = mobIndex;
        }

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetMobLocationPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdAdd"))) {

                final Block block = selectedMobLocations.get(player);
                if (block != null) {

                    final Location loc = block.getLocation();

                    questMob.setSpawnLocation(loc);
                    selectedMobLocations.remove(player);

                } else {
                    player.sendMessage(ColorUtil.RED + Lang.get("eventEditorSelectBlockFirst"));
                    return new MobLocationPrompt(mobIndex, questMob);
                }

                return new QuestMobPrompt(mobIndex, questMob);

            } else if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {

                selectedMobLocations.remove(player);
                return new QuestMobPrompt(mobIndex, questMob);

            } else {
                return new MobLocationPrompt(mobIndex, questMob);
            }

        }
    }

    private class MobDropPrompt extends StringPrompt {

        private final QuestMob questMob;
        private final Integer  mobIndex;
        private final Integer  invIndex;

        public MobDropPrompt(int invIndex, int mobIndex, QuestMob questMob) {
            this.questMob = questMob;
            this.mobIndex = mobIndex;
            this.invIndex = invIndex;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            final String text = ColorUtil.YELLOW + Lang.get("eventEditorSetDropChance");
            return text;
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            float chance;

            if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {
                return new QuestMobPrompt(mobIndex, questMob);
            }

            try {
                chance = Float.parseFloat(input);
            } catch (final NumberFormatException e) {
                context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorInvalidDropChance"));
                return new MobDropPrompt(invIndex, mobIndex, questMob);
            }
            if (chance > 1 || chance < 0) {
                context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorInvalidDropChance"));
                return new MobDropPrompt(invIndex, mobIndex, questMob);
            }

            questMob.dropChances[invIndex] = chance;

            return new QuestMobPrompt(mobIndex, questMob);
        }
    }

    private class LightningPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorLightningPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdAdd"))) {

                final Block block = selectedLightningLocations.get(player);
                if (block != null) {

                    final Location loc = block.getLocation();

                    LinkedList<String> locs;
                    if (context.getSessionData(CK.E_LIGHTNING) != null) {
                        locs = (LinkedList<String>) context.getSessionData(CK.E_LIGHTNING);
                    } else {
                        locs = new LinkedList<String>();
                    }

                    locs.add(Quests.getLocationInfo(loc));
                    context.setSessionData(CK.E_LIGHTNING, locs);
                    selectedLightningLocations.remove(player);

                } else {
                    player.sendMessage(ColorUtil.RED + Lang.get("eventEditorSelectBlockFirst"));
                    return new LightningPrompt();
                }

                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdClear"))) {

                context.setSessionData(CK.E_LIGHTNING, null);
                selectedLightningLocations.remove(player);
                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {

                selectedLightningLocations.remove(player);
                return new CreateMenuPrompt();

            } else {
                return new LightningPrompt();
            }

        }
    }

    private class PotionEffectPrompt extends FixedSetPrompt {

        public PotionEffectPrompt() {

            super("1", "2", "3", "4", "5");

        }

        @Override
        public String getPromptText(ConversationContext context) {

            String text = ColorUtil.GOLD + "- " + Lang.get("eventEditorPotionEffects") + " -\n";
            if (context.getSessionData(CK.E_POTION_TYPES) == null) {
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetPotionEffects") + " (" + Lang.get("noneSet") + ")\n";
                text += ColorUtil.GRAY + "2 - " + Lang.get("eventEditorSetPotionDurations") + " "
                        + Lang.get("eventEditorNoTypesSet") + "\n";
                text += ColorUtil.GRAY + "3 - " + Lang.get("eventEditorSetPotionMagnitudes") + " "
                        + Lang.get("eventEditorNoTypesSet") + "\n";
                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.GREEN + "" + ColorUtil.BOLD + "5" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");
            } else {

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "1" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("eventEditorSetPotionEffects") + "\n";
                for (final String s: (LinkedList<String>) context.getSessionData(CK.E_POTION_TYPES)) {
                    text += ColorUtil.GRAY + "    - " + ColorUtil.AQUA + s + "\n";
                }

                if (context.getSessionData(CK.E_POTION_DURATIONS) == null) {
                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorSetPotionDurations") + " (" + Lang.get("noneSet") + ")\n";
                    text += ColorUtil.GRAY + "3 - " + Lang.get("eventEditorSetPotionMagnitudes") + " "
                            + Lang.get("eventEditorNoDurationsSet") + "\n";
                } else {

                    text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "2" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                            + Lang.get("eventEditorNoDurationsSet") + "\n";
                    for (final Long l: (LinkedList<Long>) context.getSessionData(CK.E_POTION_DURATIONS)) {
                        text += ColorUtil.GRAY + "    - " + ColorUtil.DARKAQUA + Quests.getTime(l * 50L) + "\n";
                    }

                    if (context.getSessionData(CK.E_POTION_STRENGHT) == null) {
                        text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                                + Lang.get("eventEditorSetPotionMagnitudes") + " (" + Lang.get("noneSet") + ")\n";
                    } else {

                        text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "3" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                                + Lang.get("eventEditorSetPotionMagnitudes") + "\n";
                        for (final int i: (LinkedList<Integer>) context.getSessionData(CK.E_POTION_STRENGHT)) {
                            text += ColorUtil.GRAY + "    - " + ColorUtil.PURPLE + i + "\n";
                        }

                    }

                }

                text += ColorUtil.BLUE + "" + ColorUtil.BOLD + "4" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("clear") + "\n";
                text += ColorUtil.GREEN + "" + ColorUtil.BOLD + "5" + ColorUtil.RESET + ColorUtil.YELLOW + " - "
                        + Lang.get("done");

            }

            return text;

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("1")) {
                return new PotionTypesPrompt();
            } else if (input.equalsIgnoreCase("2")) {
                if (context.getSessionData(CK.E_POTION_TYPES) == null) {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorMustSetPotionTypesFirst"));
                    return new PotionEffectPrompt();
                } else {
                    return new PotionDurationsPrompt();
                }
            } else if (input.equalsIgnoreCase("3")) {
                if (context.getSessionData(CK.E_POTION_TYPES) == null) {
                    context.getForWhom().sendRawMessage(
                            ColorUtil.RED + Lang.get("eventEditorMustSetPotionTypesAndDurationsFirst"));
                    return new PotionEffectPrompt();
                } else if (context.getSessionData(CK.E_POTION_DURATIONS) == null) {
                    context.getForWhom().sendRawMessage(
                            ColorUtil.RED + Lang.get("eventEditorMustSetPotionDurationsFirst"));
                    return new PotionEffectPrompt();
                } else {
                    return new PotionMagnitudesPrompt();
                }

            } else if (input.equalsIgnoreCase("4")) {
                context.getForWhom().sendRawMessage(ColorUtil.YELLOW + Lang.get("eventEditorPotionsCleared"));
                context.setSessionData(CK.E_POTION_TYPES, null);
                context.setSessionData(CK.E_POTION_DURATIONS, null);
                context.setSessionData(CK.E_POTION_STRENGHT, null);
                return new PotionEffectPrompt();
            } else if (input.equalsIgnoreCase("5")) {

                int one;
                int two;
                int three;

                if (context.getSessionData(CK.E_POTION_TYPES) != null) {
                    one = ((List<String>) context.getSessionData(CK.E_POTION_TYPES)).size();
                } else {
                    one = 0;
                }

                if (context.getSessionData(CK.E_POTION_DURATIONS) != null) {
                    two = ((List<Long>) context.getSessionData(CK.E_POTION_DURATIONS)).size();
                } else {
                    two = 0;
                }

                if (context.getSessionData(CK.E_POTION_STRENGHT) != null) {
                    three = ((List<Integer>) context.getSessionData(CK.E_POTION_STRENGHT)).size();
                } else {
                    three = 0;
                }

                if (one == two && two == three) {
                    return new CreateMenuPrompt();
                } else {
                    context.getForWhom().sendRawMessage(ColorUtil.RED + Lang.get("eventEditorListSizeMismatch"));
                    return new PotionEffectPrompt();
                }

            }
            return null;

        }
    }

    private class PotionTypesPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            String effs = ColorUtil.PINK + "- " + Lang.get("eventEditorPotionEffects") + " - \n";
            for (final PotionEffectType pet: PotionEffectType.values()) {
                effs += (pet != null && pet.getName() != null) ? (ColorUtil.PURPLE + pet.getName() + "\n") : "";
            }

            return effs + ColorUtil.YELLOW + Lang.get("eventEditorSetPotionEffectsPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                final LinkedList<String> effTypes = new LinkedList<String>();
                for (final String s: input.split(" ")) {

                    if (PotionEffectType.getByName(s.toUpperCase()) != null) {

                        effTypes.add(PotionEffectType.getByName(s.toUpperCase()).getName());

                        context.setSessionData(CK.E_POTION_TYPES, effTypes);

                    } else {
                        player.sendMessage(ColorUtil.PINK + s + " " + ColorUtil.RED
                                + Lang.get("eventEditorInvalidPotionType"));
                        return new PotionTypesPrompt();
                    }

                }

            }

            return new PotionEffectPrompt();

        }
    }

    private class PotionDurationsPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetPotionDurationsPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                final LinkedList<Long> effDurations = new LinkedList<Long>();
                for (final String s: input.split(" ")) {

                    try {

                        final long l = Long.parseLong(s);

                        if (l < 1000) {
                            player.sendMessage(ColorUtil.PINK + s + " " + ColorUtil.RED
                                    + Lang.get("eventEditorNotGreaterThanOneSecond"));
                            return new PotionDurationsPrompt();
                        }

                        effDurations.add(l / 50L);

                    } catch (final NumberFormatException e) {
                        player.sendMessage(ColorUtil.PINK + s + " " + ColorUtil.RED + Lang.get("eventEditorNotANumber"));
                        return new PotionDurationsPrompt();
                    }

                }

                context.setSessionData(CK.E_POTION_DURATIONS, effDurations);

            }

            return new PotionEffectPrompt();

        }
    }

    private class PotionMagnitudesPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetPotionMagnitudesPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false) {

                final LinkedList<Integer> magAmounts = new LinkedList<Integer>();
                for (final String s: input.split(" ")) {

                    try {

                        final int i = Integer.parseInt(s);

                        if (i < 1) {
                            player.sendMessage(ColorUtil.PINK + s + " " + ColorUtil.RED
                                    + Lang.get("eventEditorNotGreaterThanZero"));
                            return new PotionMagnitudesPrompt();
                        }

                        magAmounts.add(i);

                    } catch (final NumberFormatException e) {
                        player.sendMessage(ColorUtil.PINK + s + " " + ColorUtil.RED + Lang.get("eventEditorNotANumber"));
                        return new PotionMagnitudesPrompt();
                    }

                }

                context.setSessionData(CK.E_POTION_STRENGHT, magAmounts);

            }

            return new PotionEffectPrompt();

        }
    }

    private class HungerPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetHungerPrompt");

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {

            if (input.intValue() != -1) {

                if (input.intValue() < 0) {
                    ((Player) context.getForWhom()).sendMessage(ColorUtil.RED
                            + Lang.get("eventEditorHungerLevelAtLeastZero"));
                    return new HungerPrompt();
                } else {
                    context.setSessionData(CK.E_HUNGER, input.intValue());
                }

            } else {
                context.setSessionData(CK.E_HUNGER, null);
            }

            return new CreateMenuPrompt();

        }
    }

    private class SaturationPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetSaturationPrompt");

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {

            if (input.intValue() != -1) {

                if (input.intValue() < 0) {
                    ((Player) context.getForWhom()).sendMessage(ColorUtil.RED
                            + Lang.get("eventEditorSaturationLevelAtLeastZero"));
                    return new SaturationPrompt();
                } else {
                    context.setSessionData(CK.E_SATURATION, input.intValue());
                }

            } else {
                context.setSessionData(CK.E_SATURATION, null);
            }

            return new CreateMenuPrompt();

        }
    }

    private class HealthPrompt extends NumericPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetHealthPrompt");

        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number input) {

            if (input.intValue() != -1) {

                if (input.intValue() < 0) {
                    ((Player) context.getForWhom()).sendMessage(ColorUtil.RED
                            + Lang.get("eventEditorHealthLevelAtLeastZero"));
                    return new HealthPrompt();
                } else {
                    context.setSessionData(CK.E_HEALTH, input.intValue());
                }

            } else {
                context.setSessionData(CK.E_HEALTH, null);
            }

            return new CreateMenuPrompt();

        }
    }

    private class TeleportPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            return ColorUtil.YELLOW + Lang.get("eventEditorSetTeleportPrompt");

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            final Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdDone"))) {

                final Block block = selectedTeleportLocations.get(player);
                if (block != null) {

                    final Location loc = block.getLocation();

                    context.setSessionData(CK.E_TELEPORT, Quests.getLocationInfo(loc));
                    selectedTeleportLocations.remove(player);

                } else {
                    player.sendMessage(ColorUtil.RED + Lang.get("eventEditorSelectBlockFirst"));
                    return new TeleportPrompt();
                }

                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdClear"))) {

                context.setSessionData(CK.E_TELEPORT, null);
                selectedTeleportLocations.remove(player);
                return new CreateMenuPrompt();

            } else if (input.equalsIgnoreCase(Lang.get("cmdCancel"))) {

                selectedTeleportLocations.remove(player);
                return new CreateMenuPrompt();

            } else {
                return new TeleportPrompt();
            }

        }
    }

    private class CommandsPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {

            final String text = ColorUtil.GOLD + "" + ColorUtil.ITALIC + Lang.get("eventEditorCommandsNote");
            return ColorUtil.YELLOW + Lang.get("eventEditorSetCommandsPrompt") + "\n" + text;

        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            context.getForWhom();

            if (input.equalsIgnoreCase(Lang.get("cmdCancel")) == false
                    && input.equalsIgnoreCase(Lang.get("cmdClear")) == false) {

                final String[] commands = input.split(",");
                final LinkedList<String> cmdList = new LinkedList<String>();
                cmdList.addAll(Arrays.asList(commands));
                context.setSessionData(CK.E_COMMANDS, cmdList);

            } else if (input.equalsIgnoreCase(Lang.get("cmdClear"))) {
                context.setSessionData(CK.E_COMMANDS, null);
            }

            return new CreateMenuPrompt();

        }
    }
}

