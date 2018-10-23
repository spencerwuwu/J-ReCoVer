// https://searchcode.com/api/result/122992301/

package com.aewin.bot.strategies;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.aewin.algorithms.FisherYatesShuffle;
import com.aewin.algorithms.data.BooleanBuffer2DView;
import com.aewin.algorithms.data.integer.IntBuffer2DWrapping;
import com.aewin.bot.PluggableAewinParams;
import com.aewin.bot.strategies.combat.CombatRegion;
import com.aewin.map.AntsMap;
import com.aewin.map.MapTile;
import com.aewin.pathfinding.ExpansionFunction;
import com.aewin.pathfinding.MiscAlgorithms;
import com.aewin.pathfinding.PartialPath;
import com.aewin.pathfinding.PartialPathSolution;
import com.aewin.pathfinding.PartialPathSolution.PartialPathSolutionDistanceComparator;
import com.aewin.starter.Aim;
import com.aewin.starter.Ants;
import com.aewin.starter.Order;
import com.aewin.starter.Order.OrderType;
import com.aewin.starter.TileKey;
import com.aewin.util.Callable;
import com.aewin.util.Functor.BooleanFunctor;
import com.aewin.util.Functor.FunctorRV;
import com.aewin.util.Log;
import com.aewin.util.Log.LogCategory;
import com.aewin.util.Nullable;
import com.aewin.util.crapcopiesofguava.Iterables;
import com.aewin.util.crapcopiesofguava.Lists;


public abstract class AbstractKillStrategy extends AbstractStrategy {
	
	protected Map<Integer, CombatRegion> combatRegions;
	protected final Set<MapTile> killTargets = new HashSet<MapTile>();

	public AbstractKillStrategy(StrategyBot bot) {
		super(bot);
	}
	
	public Iterable<TileKey> getRecruitDests() {
		if(killTargets == null) {
			return Lists.newArrayList();
		}
		return Iterables.map(killTargets, new FunctorRV<MapTile, TileKey>() {
			@Override
			public TileKey f(MapTile t) {
				return t.getKey();
			}
		});
	}
	
	public @Nullable Map<Integer, CombatRegion> getCombatRegions() {
		return combatRegions;
	}
	
	protected abstract void updateKillTargets();
	
	@Override
	public boolean canSkipOnFirstTurn() {
		return true;
	}

	@Override
	public void doTurn() {
		combatRegions = bot.getAnts().getMap().getCombatRegions();
		
		updateKillTargets();
		Set<TileKey> killTargetsKeys = new HashSet<TileKey>();
		Iterable<TileKey> killTargetsKeysIterable = Iterables.map(killTargets, new FunctorRV<MapTile, TileKey>() {
			@Override
			public TileKey f(MapTile t) {
				return t.getKey();
			}
		});
		for(TileKey key : killTargetsKeysIterable) {
			killTargetsKeys.add(key);
		}
		
		//ehh, this should be handled better...
		if(killTargetsKeys.size() == 0) {
			return;
		}
		
		combatRegions.clear();
		combatRegions.putAll(bot.getAnts().getMap().getCombatRegions());
		
		doTurnKillCreatePendingOrders(killTargetsKeys, combatRegions);
		
		doTurnKillEvaluateAndIssue(combatRegions);
	}

	protected void doTurnKillEvaluateAndIssue(Map<Integer, CombatRegion> combatRegions) {
		//for each combat region:
			//1) update influence based on impenetrable tiles and/or ants which will die
			//2) for each pending order:
				//if there are orders which won't be committed, remove that order from pending, and go back to 1)
			//3) move all remaining orders from pending and issue
		
		for(Entry<Integer, CombatRegion> entry : combatRegions.entrySet()) {
			boolean removed = true;
			Callable<Boolean> callable = bot.getCancellingCallable(PluggableAewinParams.TURN_OVER_FOR_COMBAT_MS_STATIC);
			
			while(removed && entry.getValue().getOrders().size() > 0) {
				if(callable.call()) {
					return;
				}
				
				removed = false;
				
				CombatRegion cr = entry.getValue();
				
				fillInfluences(cr);
				fillInfluencesFromMe(cr);
				List<Order> unsafe =  getUnsafeOrders(cr);
				
				for(Order o : unsafe) {
					cr.removeOrder(o);
					removed = true;
				}
			}
		}
		
		for(Entry<Integer, CombatRegion> entry : combatRegions.entrySet()) {
			for(Order order : entry.getValue().getOrders()) {
				bot.issueOrder(order.getSolution().getStart(), order);
			}
		}
	}

	private List<Order> getUnsafeOrders(CombatRegion cr) {
		List<Order> unsafe = Lists.newArrayList();
		
		List<Order> orders = cr.getOrders();
		orderFor: for(Order order : orders) {
			Iterator<TileKey> iterator = order.getSolution().getPathIterable().iterator();
			TileKey nextTile = iterator.next();	//start tile
			if(iterator.hasNext()) {
				nextTile = iterator.next();
			}
			
			AntsMap map = bot.getAnts().getMap();
			MapTile myAntNewTile = map.getTile(nextTile);
			
			Iterable<MapTile> enemyTiles = Iterables.filter(myAntNewTile.getSquaresThatCanAttackHereWithMove(), new BooleanFunctor<MapTile>() {
				@Override
				public boolean f(MapTile t) {
					return t.ant != null && !t.ant.getPlayer().isMe();
				}
			});
			
			for(MapTile other : enemyTiles) {
				//can be >, which causes trades
				int myNewInfluence = cr.myNewInfluenceAtTile.get(other.getKey().x, other.getKey().y);
				
				int myDanger = cr.myDangerAtTileAfterMoveRestriction.get(myAntNewTile.getKey().x, myAntNewTile.getKey().y);
				
				boolean disallowed = false;
				
				if(myDanger > 0) {
					if(myNewInfluence < myDanger) {
						disallowed = true;
					} else if (myNewInfluence == myDanger) {
						if(!bot.getParams().allowTradesInCombat || myNewInfluence == 1) {
							disallowed = true;
						}
					}
				}
				
				if(disallowed) {
					unsafe.add(order);
					continue orderFor;
				}
			}
		}
		
		return unsafe;
	}

	private void fillInfluencesFromMe(CombatRegion cr) {
		final Ants ants = bot.getAnts();
		
		for(Order o : cr.getOrders()) {
			Iterator<TileKey> iterator = o.getSolution().getPathIterable().iterator();
			TileKey nextTile = iterator.next();	//start tile
			if(iterator.hasNext()) {
				nextTile = iterator.next();
			}
			
			//shouldn't really be attackWithMove, but then you need to have logic to account for enemies that won't move because of death
			//that has problems too... hmmm...
			addAntInfluence(nextTile.x, nextTile.y, cr.myNewInfluenceAtTile, ants.getAttackKernel());
		}
		
		final AntsMap map = ants.getMap();

		for(MapTile tile : bot.getOtherAntTiles()) {
			{
				int otherInfluence = tile.getInfluenceWithMoveForPlayer(tile.ant.getPlayer());
				int myNewInfluence = cr.myNewInfluenceAtTile.get(tile.getKey().x, tile.getKey().y);
				
				if(myNewInfluence > otherInfluence) {
					BooleanBuffer2DView buff = ants.getAttackKernel();
					
					removeAntInfluence(tile.getKey().x, tile.getKey().y, cr.myDangerAtTileAfterMoveRestriction, buff);
				}
			}
			
			for(MapTile adj : tile.getAdjacentTilesExcludingMyself()) {
				int otherInfluence = adj.getInfluenceWithMoveForPlayer(tile.ant.getPlayer());
				int myNewInfluence = cr.myNewInfluenceAtTile.get(adj.getKey().x, adj.getKey().y);
				
				if(myNewInfluence > otherInfluence) {
					BooleanBuffer2DView buff;
					Aim aim = Aim.fromTilekeys(tile.getKey(), adj.getKey(), map.getWidth(), map.getHeight());
					
					switch(aim) {
					case NORTH: {
						buff = ants.getAttackWithNorthMoveOnlyKernel();
						break;
					}
					case SOUTH: {
						buff = ants.getAttackWithSouthMoveOnlyKernel();
						break;
					}
					case EAST: {
						buff = ants.getAttackWithRightMoveOnlyKernel();
						break;
					}
					case WEST: {
						buff = ants.getAttackWithLeftMoveOnlyKernel();
						break;
					}
					default: {
						throw new RuntimeException("Unexpected aim!!!");
					}
					}
					
					removeAntInfluence(tile.getKey().x, tile.getKey().y, cr.myDangerAtTileAfterMoveRestriction, buff);
				}
			}
		}
	}

	private void fillInfluences(CombatRegion cr) {
		final AntsMap map = bot.getAnts().getMap();
		
		//TODOLT: could make this faster by only filling inside CR? maybe?
		//dunno, maybe this is better, and then we only use CR to determine what to move
		
		if(cr.myDangerAtTileAfterMoveRestriction != null) {
			cr.myDangerAtTileAfterMoveRestriction.clear();
		} else {
			cr.myDangerAtTileAfterMoveRestriction = new IntBuffer2DWrapping(map.getWidth(), map.getHeight());
		}
		
		for(MapTile tile : map) {
			cr.myDangerAtTileAfterMoveRestriction.set(tile.getKey().x, tile.getKey().y, tile.getMyMaxEnemyInfluenceWithMove());
		}
		
		if(cr.myNewInfluenceAtTile != null) {
			cr.myNewInfluenceAtTile.clear();
		} else {
			cr.myNewInfluenceAtTile = new IntBuffer2DWrapping(map.getWidth(), map.getHeight());
		}
	}
	
	private void removeAntInfluence(int x, int y, IntBuffer2DWrapping influence, BooleanBuffer2DView buffer) {
		int size_2 = buffer.width/2;
		
		for(int yDelta=-size_2; yDelta<=size_2; yDelta++) {
			for(int xDelta=-size_2; xDelta<=size_2; xDelta++) {
				if(buffer.get(xDelta+size_2, yDelta+size_2)) {
					int xx = x + xDelta;
					int yy = y + yDelta;
					
					int i = influence.get(xx, yy);
					influence.set(xx, yy, --i);
				}
			}
		}
	}
	
	private void addAntInfluence(int x, int y, IntBuffer2DWrapping influence, BooleanBuffer2DView buffer) {
		int size_2 = buffer.width/2;
		
		for(int yDelta=-size_2; yDelta<=size_2; yDelta++) {
			for(int xDelta=-size_2; xDelta<=size_2; xDelta++) {
				if(buffer.get(xDelta+size_2, yDelta+size_2)) {
					int xx = x + xDelta;
					int yy = y + yDelta;
					
					int i = influence.get(xx, yy);
					influence.set(xx, yy, ++i);
				}
			}
		}
	}

	protected void doTurnKillCreatePendingOrders(Iterable<TileKey> killTargetsKeys, Map<Integer, CombatRegion> combatRegions) {
		final Ants ants = bot.getAnts();
		final AntsMap map = ants.getMap();
		
		final List<PartialPathSolution> potentials = Lists.newArrayList();
		
		final Set<TileKey> pending = new HashSet<TileKey>();
		
		BooleanFunctor<MapTile> functor = new BooleanFunctor<MapTile>() {
			@Override
			public boolean f(MapTile tile) {
				boolean rv = tile.isPassable();
				rv &= (tile.ant == null || !tile.ant.getPlayer().isMe() || tile.combatRegion == 0);
				rv &= !pending.contains(tile.getKey());
				return rv;
			}
		};

		ExpansionFunction expansionFunction = MiscAlgorithms.expansionFunctionRandomized(bot.getRandom(), map, functor);
		Set<TileKey> goals = new HashSet<TileKey>();
		for(TileKey key : killTargetsKeys) {
			goals.add(key);
		}
		
		for(TileKey goal : Lists.newArrayList(goals)) {
			List<MapTile> squaresThatCanAttackHere = map.getTile(goal).getSquaresThatCanAttackHere();
			Iterable<TileKey> newGoals = Iterables.map(squaresThatCanAttackHere, new FunctorRV<MapTile, TileKey>() {
				public TileKey f(MapTile t) {
					return t.getKey();
				};
			});
			goals.addAll(Lists.newArrayList(newGoals));
		}
		
		//queue up kill orders for eval
		//the order here might not be optimal; might need to rethink!
		//actually, going to shuffle to reduce bias -_-
		Iterable<MapTile> tiles = bot.getMyAntTiles();
		List<MapTile> tilesRandom = Lists.newArrayList(tiles);
		FisherYatesShuffle.shuffleInPlace(bot.getRandom(), tilesRandom);
		
		/*
		 * first, we solve allowing them to walk over each other, so we can find the closest ones
		 */
		{
			PartialPath pp = new PartialPath(map.getWidth(), map.getHeight(), expansionFunction, goals);
			myAntFor: for(MapTile tile : tilesRandom) {
				if(OrderType.KILL.isMoreImportantThan(tile.ant.asMyAnt().getCurrentOrder())) {
					if(tile.combatRegion == 0) {
						continue myAntFor;
					}
					
					pp.setMaxDepth(ants.getViewRadius2());
					PartialPathSolution pps = pp.solveFrom(tile.getKey(), bot.getCancellingCallable(PluggableAewinParams.TURN_OVER_FOR_COMBAT_MS_STATIC));
					if(pps.getEnd() == null) {
						Log.debug("Can't kill from here, no path: " + tile.getKey(), LogCategory.DEBUG);
						continue;
					}
					
					potentials.add(pps);
				}
			}
		}
		
		Collections.sort(potentials, new PartialPathSolutionDistanceComparator());
		
		/*
		 * now, we go through, pathing the closest ones first so they'll get optimal
		 * paths and further away ones can go around
		 */
		
		//this is a hack, but it's slow if there's a blocked path for a combat region...
		HashSet<Integer> blockedCombatRegions = new HashSet<Integer>();
		
		for(PartialPathSolution pps : potentials) {
			if(blockedCombatRegions.contains(map.getTile(pps.getStart()).combatRegion)) {
				Log.debug("Can't  kill because couldn't path the second time once before: " + pps.getStart(), LogCategory.DEBUG);
				continue;
			}
			PartialPath pp = new PartialPath(map.getWidth(), map.getHeight(), expansionFunction, goals);
			pp.setMaxDepth(ants.getViewRadius2());
			pps = pp.solveFrom(pps.getStart(), bot.getCancellingCallable(PluggableAewinParams.TURN_OVER_FOR_COMBAT_MS_STATIC));
			if(pps.getEnd() == null) {
				Log.debug("Can't kill from here, no path the second time: " + pps.getStart(), LogCategory.DEBUG);
				blockedCombatRegions.add(map.getTile(pps.getStart()).combatRegion);
				continue;
			}
			
			Iterator<TileKey> iterator = pps.getPathIterable().iterator();
			iterator.next();	//start
			pending.add(iterator.next());
			
			//the int here is ignored; special kill logic determines if it will issue
			Order order = new Order(OrderType.KILL, pps.getEnd(), Integer.MAX_VALUE, pps, true);
			
			int combatRegion = map.getTile(pps.getEnd()).combatRegion;
			
			CombatRegion cr = combatRegions.get(combatRegion);
			
			cr.addOrder(order);
		}
	}
}
