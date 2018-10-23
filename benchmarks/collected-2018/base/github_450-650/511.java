// https://searchcode.com/api/result/110347556/

/**
 * 
 * CraigStars!
 * 
 * Copyright (c) 2010 Craig Post Licensed under the MIT license.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * 
 */
package net.craigstars.game.services.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.craigstars.game.ai.ColonizerTurnProcessor;
import net.craigstars.game.ai.ScoutTurnProcessor;
import net.craigstars.game.ai.TurnProcessor;
import net.craigstars.game.services.FleetController;
import net.craigstars.game.services.PlanetController;
import net.craigstars.model.Fleet;
import net.craigstars.model.Game;
import net.craigstars.model.MapObject;
import net.craigstars.model.Message;
import net.craigstars.model.Planet;
import net.craigstars.model.Player;
import net.craigstars.model.enums.GameStatus;
import net.craigstars.model.enums.TechField;
import net.craigstars.model.tech.TechPlanetaryScanner;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle generating turns
 * It also exposes methods for universe generation, like scanning and turn processing.
 */
public class TurnGenerator {

    private static final Logger log = LoggerFactory.getLogger(TurnGenerator.class);

    /**
     * Helper class to represent a scanner for scanning
     * 
     */
    private class Scanner {

        private MapObject mapObject;
        private int scanRange;
        private int scanRangePen;

        public Scanner(MapObject mapObject, int scanRange, int scanRangePen) {
            super();
            this.mapObject = mapObject;
            this.scanRange = scanRange;
            this.scanRangePen = scanRangePen;
        }

        public MapObject getMapObject() {
            return mapObject;
        }

        public int getScanRange() {
            return scanRange;
        }

        public int getScanRangePen() {
            return scanRangePen;
        }

    }

    private Game game;
    private FleetController fleetController;
    private PlanetController planetController;

    public TurnGenerator(Game game, FleetController fleetController, PlanetController planetController) {
        super();
        this.game = game;
        this.fleetController = fleetController;
        this.planetController = planetController;
    }

    /**
     * <pre>
     *         Generate a turn
     * 
     *         Stars! Order of Events 
     *         
     *         Scrapping fleets (w/possible tech gain) 
     *         Waypoint 0 unload tasks 
     *         Waypoint 0 Colonization/Ground Combat resolution (w/possible tech gain) 
     *         Waypoint 0 load tasks 
     *         Other Waypoint 0 tasks * 
     *         MT moves 
     *         In-space packets move and decay 
     *         PP packets (de)terraform 
     *         Packets cause damage 
     *         Wormhole entry points jiggle 
     *         Fleets move (run out of fuel, hit minefields (fields reduce as they are hit), stargate, wormhole travel) 
     *         Inner Strength colonists grow in fleets 
     *         Mass Packets still in space and Salvage decay 
     *         Wormhole exit points jiggle 
     *         Wormhole endpoints degrade/jump 
     *         SD Minefields detonate (possibly damaging again fleet that hit minefield during movement) 
     *         Mining 
     *         Production (incl. research, packet launch, fleet/starbase construction) 
     *         SS Spy bonus obtained 
     *         Population grows/dies 
     *         Packets that just launched and reach their destination cause damage 
     *         Random events (comet strikes, etc.) 
     *         Fleet battles (w/possible tech gain) 
     *         Meet MT 
     *         Bombing 
     *         Waypoint 1 unload tasks 
     *         Waypoint 1 Colonization/Ground Combat resolution (w/possible tech gain) 
     *         Waypoint 1 load tasks 
     *         Mine Laying 
     *         Fleet Transfer 
     *         CA Instaforming 
     *         Mine sweeping 
     *         Starbase and fleet repair 
     *         Remote Terraforming
     * </pre>
     */
    public void generate() {
        // time this logic
        StopWatch timer = new StopWatch();
        timer.start();

        game.setYear(game.getYear() + 1);

        // initialize the players to starting turn state
        initPlayers();

        performWaypointTasks(0);
        moveMysteryTrader();
        movePackets();
        updateWormholeEntryPoints();
        moveFleets();
        growISFleets();
        decayPackets();
        updateWormholeExitPoints();
        detonateMinefields();
        mine();
        produce();
        ssSpy();
        grow();
        randomEvents();
        battle();
        mysteryTrader();
        bombing();
        performWaypointTasks(0);
        layMines();
        transferFleets();
        terraformCAPlanets();
        sweepMines();
        repairFleets();
        remoteTerraform();

        scan(game);

        // do turn processing
        processTurns(game);

        /*
         * for (Planet planet : game.getPlanets()) { dao.getPlanetDao().save(planet);
         * dao.getProductionQueueDao().save(planet.getQueue()); }
         * 
         * for (Fleet fleet : game.getFleets()) { dao.getFleetDao().save(fleet); }
         */

        game.setStatus(GameStatus.WaitingForSubmit);
        log.info("Generated new turn {}, {} ms", game.getYear(), timer.getTime());

    }

    /**
     * Initialize the players to turn start values
     */
    private void initPlayers() {
        // clear out the player messages
        for (Player player : game.getPlayers().values()) {
            for (Message message : player.getMessages()) {
                message.setTarget(null);
            }
            player.getMessages().clear();
            player.getFleetKnowledges().clear();
            player.setSubmittedTurn(false);
        }
    }

    /**
     * Perform any waypoint tasks, including scrapping and tech gain and do it in a specific order
     * <ul>
     * <li>Scrapping fleets (w/possible tech gain)</li>
     * <li>Waypoint 0 unload tasks</li>
     * <li>Waypoint 0 Colonization/Ground Combat resolution (w/possible tech gain)</li>
     * <li>Waypoint 0 load tasks</li>
     * <li>Other Waypoint 0 tasks</li>
     * </ul>
     */
    private void performWaypointTasks(int index) {
        List<Fleet> scrapFleets = new ArrayList<Fleet>();
        List<Fleet> unloadFleets = new ArrayList<Fleet>();
        List<Fleet> colonizeFleets = new ArrayList<Fleet>();
        List<Fleet> loadFleets = new ArrayList<Fleet>();
        List<Fleet> otherFleets = new ArrayList<Fleet>();

        for (Fleet fleet : game.getFleets()) {
            switch (fleet.getWaypoints().get(index).getTask()) {
            case Colonize:
                colonizeFleets.add(fleet);
                break;
            case ScrapFleet:
                scrapFleets.add(fleet);
                break;
            case Transport:
                unloadFleets.add(fleet);
                // loadFleets.add(fleet);
                break;
            case TransferFleet:
                // this occurs later
                break;
            default:
                otherFleets.add(fleet);
                break;

            }
        }

        for (Fleet fleet : scrapFleets) {
            fleetController.processTask(fleet, fleet.getWaypoints().get(index));
        }

        for (Fleet fleet : unloadFleets) {
            fleetController.processTask(fleet, fleet.getWaypoints().get(index));
        }

        // resolve ground combat
        // TODO: figure this out
        // perhaps each unload command unloads pop into a separate collection
        // that is transient to the DB
        // then we call the planetController to resolve ground combat on each planet
        // that needs it, in the end the remaining winner's pop is transferred to the real pop
        // ownership of the planet is transferred and viola, done
        // this way, each player dropping pop on a planet gets a chance to take it

        for (Fleet fleet : colonizeFleets) {
            fleetController.processTask(fleet, fleet.getWaypoints().get(index));
        }

        for (Fleet fleet : loadFleets) {
            fleetController.processTask(fleet, fleet.getWaypoints().get(index));
        }

        for (Fleet fleet : otherFleets) {
            fleetController.processTask(fleet, fleet.getWaypoints().get(index));
        }
        
        if (index != 0) {
            for (Fleet fleet : game.getFleets()) {
                // we've arrived, remove the waypoint
                fleet.getWaypoints().remove(0);
                if (fleet.getWaypoints().size() == 1) {
                    Message.fleetCompletedAssignedOrders(fleet.getOwner(), fleet);
                }
            }
            
        }

    }

    private void moveMysteryTrader() {
        // TODO Auto-generated method stub

    }

    private void movePackets() {
        // TODO Auto-generated method stub

    }

    private void updateWormholeEntryPoints() {
        // TODO Auto-generated method stub

    }

    private void growISFleets() {
        // TODO Auto-generated method stub

    }

    private void decayPackets() {
        // TODO Auto-generated method stub

    }

    private void updateWormholeExitPoints() {
        // TODO Auto-generated method stub

    }

    private void detonateMinefields() {
        // TODO Auto-generated method stub

    }

    /**
     * Mine on any planet, mine with remote miners
     */
    private void mine() {
        // generate each planet turn
        for (Planet planet : game.getPlanets()) {
            if (planet.getOwner() != null) {
                planetController.mine(planet);
            }
        }
        
        for (Fleet fleet : game.getFleets()) {
            if (!fleet.isScrapped()) {
                // remote mine
            }
        }
    }

    /**
     * Build things on the planet, do research, etc.
     */
    private void produce() {
        Map<Player, Integer> leftoverResources = new HashMap<Player, Integer>();
        for (Player player : game.getPlayers().values()) {
            leftoverResources.put(player, 0);
        }
        // generate each planet turn
        for (Planet planet : game.getPlanets()) {
            if (planet.getOwner() != null) {
                planetController.mine(planet);
                int leftover = planetController.build(planet) + planet.getResourcesPerYearResearch();
                leftoverResources.put(planet.getOwner(), leftoverResources.get(planet.getOwner()) + leftover);

                planetController.grow(planet);
                planet.getOwner().discover(planet);
            }
        }

        // research for each player
        for (Player player : leftoverResources.keySet()) {
            research(player, leftoverResources.get(player));
        }
    }

    private void ssSpy() {
        // TODO Auto-generated method stub

    }

    /**
     * Grow population
     */
    private void grow() {
        // generate each planet turn
        for (Planet planet : game.getPlanets()) {
            if (planet.getOwner() != null) {
                planetController.grow(planet);
            }
        }
    }

    private void randomEvents() {
        // TODO Auto-generated method stub

    }

    private void battle() {
        // TODO Auto-generated method stub

    }

    private void mysteryTrader() {
        // TODO Auto-generated method stub

    }

    private void bombing() {
        // TODO Auto-generated method stub

    }

    private void layMines() {
        // TODO Auto-generated method stub

    }

    private void transferFleets() {
        // TODO Auto-generated method stub

    }

    private void terraformCAPlanets() {
        // TODO Auto-generated method stub

    }

    private void sweepMines() {
        // TODO Auto-generated method stub

    }

    private void repairFleets() {
        // TODO Auto-generated method stub

    }

    private void remoteTerraform() {
        // TODO Auto-generated method stub

    }

    /**
     * Move fleets, fleets run out of fuel, etc.
     */
    private void moveFleets() {
        // move any fleets
        List<Fleet> fleets = new ArrayList<Fleet>(game.getFleets());
        for (Fleet fleet : fleets) {
            fleetController.move(fleet);
            if (fleet.isScrapped()) {
                for (Player player : game.getPlayers().values()) {
                    player.getFleetKnowledges().remove(fleet.getId());
                    for (Message message : player.getMessages()) {
                        if (message.getTarget() != null && message.getTarget().getId().equals(fleet.getId())) {
                            message.setTarget(null);
                        }
                    }
                }
                game.getFleets().remove(fleet);
            }
        }

    }

    /**
     * Do all scan operations for the players
     * 
     * @param game The game to do scan operations for
     */
    public void scan(Game game) {
        for (Player player : game.getPlayers().values()) {
            scanPlayer(player);
        }
    }

    /**
     * Perform scanning for a player
     * 
     * @param player The player to scan for
     */
    private void scanPlayer(Player player) {

        // initialize the planetary scanner ranges
        TechPlanetaryScanner planetaryScanner = player.getTechs().getBestPlanetaryScanner();
        int planetScanRange = (int) Math.pow(planetaryScanner.getScanRange(), 2);
        int planetScanRangePen = (int) Math.pow(planetaryScanner.getScanRangePen(), 2);

        // get a list of all the fleets and planets we can discover
        List<Fleet> fleets = new ArrayList<Fleet>();
        List<Planet> planets = new ArrayList<Planet>();
        List<Scanner> scanners = new ArrayList<Scanner>();

        // find all fleets that need to be scanned, or who act as scanners
        for (Fleet fleet : player.getGame().getFleets()) {
            if (fleet.getOwner().getId() == player.getId()) {
                // this is our fleet, discover it
                fleet.discover(player, true);
                if (fleet.canScan()) {
                    scanners.add(new Scanner(fleet, (int) Math.pow(fleet.getAggregate().getScanRange(), 2), (int) Math.pow(fleet.getAggregate()
                                                                                                                                    .getScanRangePen(), 2)));
                }
            } else {
                // remove any existing knowledge and add this fleet to the list of unknowns
                // fleet.getFleetKnowledges().remove(player.getId());
                fleets.add(fleet);
            }
        }

        // find all planets that need to be scanned, or who act as scanners
        for (Planet planet : player.getGame().getPlanets()) {
            if (planet.getOwner() != null && planet.getOwner().getId() == player.getId()) {
                player.discover(planet);
                if (planet.isScanner()) {
                    scanners.add(new Scanner(planet, planetScanRange, planetScanRangePen));
                }
            } else {
                planets.add(planet);
            }
        }

        // scan with each of our scanners
        for (Scanner scanner : scanners) {

            // scan planets
            int index = 0;
            while (index < planets.size()) {
                Planet planetToScan = planets.get(index);

                // if this planet to scan is within range, have the owner of 'planet' discover it
                if (scanner.getMapObject().dist(planetToScan) <= scanner.getScanRangePen()) {
                    player.discover(planetToScan);

                    // discover any orbiting fleets
                    for (Fleet fleet : planetToScan.getOrbitingFleets()) {
                        if (fleet.getOwner().getId() != player.getId()) {
                            fleet.discover(player, true);
                            fleets.remove(fleet);
                        }
                    }
                    planets.remove(index);
                    index--;
                }
                index++;
            }

            // scan fleets
            index = 0;
            while (index < fleets.size()) {
                Fleet fleetToScan = fleets.get(index);

                // if this planet to scan is within range, have the owner of 'planet' discover it
                if (scanner.getMapObject().dist(fleetToScan) <= scanner.getScanRangePen()) {
                    fleetToScan.discover(player, true);

                    fleets.remove(index);
                    index--;
                }
                index++;
            }

            // scan fleets
            index = 0;
            while (index < fleets.size()) {
                Fleet fleetToScan = fleets.get(index);

                // if this planet to scan is within range, have the owner of 'planet' discover it
                if (scanner.getMapObject().dist(fleetToScan) <= scanner.getScanRange()) {
                    fleetToScan.discover(player, false);

                    fleets.remove(index);
                    index--;
                }
                index++;
            }

        }

    }

    /**
     * Apply a given number of resources to research possibly gaining tech levels
     * 
     * @param player The player to research
     * @param resources The resouces to apply
     */
    void research(Player player, int resources) {
        int currentSpent = player.getTechLevelsSpent().level(player.getCurrentResearchField());
        int currentLevel = player.getTechLevels().level(player.getCurrentResearchField());
        int nextLevelCost = player.getRace().getResearchCostForLevel(player.getCurrentResearchField(), currentLevel + 1);
        currentSpent += resources;

        // if we spent enough for a new level,
        // - gain the level
        // - reset our spent amount for the current level to 0
        // - set next next research level
        // - spend any leftovers again on research
        if (currentSpent >= nextLevelCost) {
            player.getTechLevels().setLevel(player.getCurrentResearchField(), currentLevel + 1);
            TechField nextField = player.getNextField();
            Message.techLevel(player, player.getCurrentResearchField(), currentLevel + 1, nextField);
            player.getTechLevelsSpent().setLevel(player.getCurrentResearchField(), 0);
            player.setCurrentResearchField(nextField);
            research(player, currentSpent - nextLevelCost);
        } else {
            // save back out how much we've spent
            player.getTechLevelsSpent().setLevel(player.getCurrentResearchField(), currentSpent);
        }

    }

    /**
     * Process turns using the turn processors
     * 
     * @param game The game to process turns for
     */
    public void processTurns(Game game) {
        for (Player player : game.getPlayers().values()) {
            if (player.isAi()) {
                TurnProcessor processor = new ScoutTurnProcessor();
                processor.init(player);
                processor.process();
    
                processor = new ColonizerTurnProcessor();
                processor.init(player);
                processor.process();
    
                if (player.isAi()) {
                    player.setSubmittedTurn(true);
                }
            }
        }

    }

}

