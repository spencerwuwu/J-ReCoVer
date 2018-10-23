// https://searchcode.com/api/result/66160569/

import com.sun.org.apache.xalan.internal.xslt.*;
import ohm.roth.*;
import ohm.roth.astar.AStarAlgorithm;
import ohm.roth.astar.AStarResult;
import ohm.roth.dorenda.DORENDABuilder;
import ohm.roth.gpx.GPXBuilder;
import ohm.roth.tsp.TSP;

import java.io.*;
import java.lang.Process;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Klasse die den Programmablauf steuert
 */

@SuppressWarnings({"ConstantConditions"})
public class Application {
    static Scanner scan = new Scanner(System.in);
    static boolean verbose = true;
    static NavGraph graph;
    static geoDatabaseConnection connection;

    static String dbhost;
    static int dbport;
    static String dbuser;
    static String dbpasswd;
    static String dbname;
    static String graphname;
    static String boundingBoxFile;
    static String lsiWeightsFilename = "lsi_foot.csv";
    static int defaultW = 1;
    static double defaultDoubleEdgeFactor = 20;

    static TSP.tspType defaultTspType = TSP.tspType.GENETIC_GOOD;
    static HashMap<String, Double> lsiWeights;

    /**
     * Ausgabe des Hauptmenues
     */
    public static void mainMenu() {
        System.out.println("============================================================");
        System.out.println("Roth LBS Aufgabe 2011");
        System.out.println("Fuchs Daniel, Rohlederer Mischa");
        System.out.println("============================================================");
        System.out.println();
        AStarAlgorithm.defaultW = Application.defaultW;
        AStarAlgorithm.visitedW = Application.defaultDoubleEdgeFactor;


        // Default Graph
        File file = new File(Application.graphname + ".DAT");
        if (file.exists()) {
            Application.loadGraph_File(graphname);
        } else {
            Application.loadGraph_Database(boundingBoxFile, graphname);
        }

        // LSI Weights
        try {
            Application.lsiWeights = new HashMap<String, Double>();
            lsiWeights = CSV.readLSIWeight(Application.lsiWeightsFilename);
        } catch (Exception e) {
            System.out.println("Error reading " + Application.lsiWeightsFilename);
        }

        boolean quit = false;
        do {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("Please Make a selection:");
            System.out.println("Loaded Graph: " + graph.getName());
            System.out.println("============================================================");
            System.out.println("[1] Build Path");
            System.out.println("[2] Benchmarks");
            System.out.println("[3] Graph");
            System.out.println("[4] Scriting");
            System.out.println("[0] exit");
            System.out.print("Select: ");
            int menu = scan.nextInt();
            switch (menu) {
                case 1:
                    Application.pathMenu();
                    break;
                case 2:
                    benchmarkMenu();
                    break;
                case 3:
                    graphMenu();
                    break;
                case 4:
                    try {
                        scriptMenu();
                    } catch (Exception e) {
                        System.out.println("Error running Script1");
                    }
                    break;
                case 0:
                    quit = true;
                    break;
            }
        }
        while (!quit);
        System.exit(0);
    }

    /**
     * Ausgabe des Menues um einen Pfad zu suchen
     */
    public static void pathMenu() {
        boolean quit = false;
        do {
            System.out.println("==================================================");
            System.out.println("Building Path Menu");
            System.out.println("==================================================");
            System.out.println();
            System.out.println("Please Make a selection:");
            System.out.println("[1] Standard");
            System.out.println("[2] Reduce Double Edges");
            System.out.println("[3] Reduce Double Edges & use LSI Weights");
            System.out.println("[0] back");
            System.out.print("Selection: ");
            int menu = scan.nextInt();
            String name, filename;
            double w;
            int limit;
            switch (menu) {
                case 1:
                    System.out.print("Name: ");
                    name = scan.next();
                    System.out.print("Filename: ");
                    filename = scan.next();
                    System.out.print("W: ");
                    w = scan.nextDouble();
                    System.out.print("Open List Limit: ");
                    limit = scan.nextInt();
                    try {
                        Application.findPath(filename, name, w, limit, true, true, false);
                    } catch (Exception e) {
                        System.out.println("Error running A-Star: " + e.getMessage());
                    }
                    break;
                case 2:
                    System.out.print("Name: ");
                    name = scan.next();
                    System.out.print("Filename: ");
                    filename = scan.next();
                    System.out.print("W: ");
                    w = scan.nextDouble();
                    System.out.print("Open List Limit: ");
                    limit = scan.nextInt();
                    try {
                        Application.findPath(filename, name, w, limit, true, true, false);
                    } catch (Exception e) {
                        System.out.println("Error running A-Star: " + e.getMessage());
                    }
                    break;
                case 3:
                    System.out.print("Name: ");
                    name = scan.next();
                    System.out.print("Filename: ");
                    filename = scan.next();
                    System.out.print("W: ");
                    w = scan.nextDouble();
                    System.out.print("Open List Limit: ");
                    limit = scan.nextInt();
                    try {
                        Application.findPath(filename, name, w, limit, true, true, true);
                    } catch (Exception e) {
                        System.out.println("Error running A-Star: " + e.getMessage());
                    }
                    break;
                case 0:
                    quit = true;
                    break;
                default:
                    System.out.println("Invalid Entry!");
            }
        }
        while (!quit);
    }

    /**
     * Sucht einen Rundweg zu Coordinaten aus einer angegebenen CSV Datei und gib Informationen zum gefundenen Pfad aus
     *
     * @param filename          Dateiname fuer die CSV Datei mit den Coordinaten
     * @param name              Name fuer den Pfad
     * @param w                 overdo Fakor fuer A-Stern
     * @param limit             Limit der Openliste
     * @param loop              Gibt  an ob ein Rundweg erstellt werden soll
     * @param reduceDoubleEdges Gibt an ob bereits besuchte Kanten schlechter Bewertet werden sollen
     * @param weightedLsi       Gib an ob bestimmte LSI-Klassen anders bewertet werden sollen
     * @throws Exception        IO Fehler
     */
    public static void findPath(String filename, String name, double w, int limit, boolean loop, boolean reduceDoubleEdges, boolean weightedLsi) throws Exception {
        Application.println("==================================================");
        Application.println("Finding Path");
        Application.println("Filename: " + filename);
        Application.println("w: " + w);
        Application.println("==================================================");

        // Load Waypoints
        double time_waypoints = System.currentTimeMillis();
        List<Waypoint> waypoints;
        try {
            waypoints = CSV.readWaypointList(filename);
        } catch (Exception e) {
            throw new Exception("Waypoint File not found");
        }
        if (waypoints.size() == 0) return;
        List<Waypoint> orderdWaypoints;
        time_waypoints = (System.currentTimeMillis() - time_waypoints) / 1000;
        Application.println("Loading Waypoints took " + time_waypoints + " Secs");
        Application.println("");

        // Rundweg
        double time_tsp = 0;
        if (waypoints.size() > 2) {
            time_tsp = System.currentTimeMillis();
            Application.println("Running TSP with " + (waypoints.size()) + " Points");
            orderdWaypoints = TSP.sort(waypoints, defaultTspType);
            time_tsp = (System.currentTimeMillis() - time_tsp) / 1000;
            Application.println("TSP took " + time_tsp + " Secs");
            Application.println("");
        } else {
            orderdWaypoints = waypoints;
        }
        try {
            FileWriter fstream = new FileWriter(new File(name + "_TSP.GPX"));
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(GPXBuilder.build(orderdWaypoints, ""));
            out.close();
        } catch (IOException e) {
            Application.println("Error writing GPX file");
        }


        // Cosest points
        System.out.println("Finding nearest Node for " + (orderdWaypoints.size()) + " Points");
        double time_nearest = System.currentTimeMillis();
        Node[] waynotes = new Node[orderdWaypoints.size()];
        for (int i = 0; i < orderdWaypoints.size(); i++) {
            waynotes[i] = graph.findClosest(orderdWaypoints.get(i), true);
        }
        time_nearest = (System.currentTimeMillis() - time_nearest) / 1000;
        System.out.println("Finding Nodes took " + time_nearest + " Secs");
        System.out.println("");

        // A-Stern
        double globalLength = 0;
        Path path = new Path(name);
        double time_astar = System.currentTimeMillis();
        System.out.println("Running A-Star " + (orderdWaypoints.size() - 1) + " times");
        List<String> visitedEdges = new ArrayList<String>();
        if (orderdWaypoints.size() > 2) {
            // A Stern im Rundweg
            int loopcount;
            if (loop) loopcount = orderdWaypoints.size() - 1;
            else loopcount = orderdWaypoints.size() - 2;
            try {
                for (int i = 0; i < loopcount; i++) {
                    Node start = waynotes[i];
                    Node end = waynotes[(i + 1) % (orderdWaypoints.size())];
                    String segmentName = orderdWaypoints.get(i).getName() + " - " + orderdWaypoints.get((i + 1) % (orderdWaypoints.size())).getName();
                    AStarResult result = AStarAlgorithm.search(graph, start, end, segmentName, w, limit, Application.lsiWeights, weightedLsi, visitedEdges, reduceDoubleEdges);
                    result.print();

                    PathSegment segment = result.getPath();
                    segment.setStartWaypoint(orderdWaypoints.get(i));
                    segment.setEndWaypoint(orderdWaypoints.get((i + 1) % (orderdWaypoints.size())));
                    globalLength += distance.distanceLinestring(segment.getSegmentLine(true));
                    path.addSegment(segment);
                }
            } catch (Exception e) {
                System.out.println("Error running A-Star: " + e.toString());
                return;
            }
        } else {
            try {
                Node start = waynotes[0];
                Node end = waynotes[1];
                String segmentName = orderdWaypoints.get(0).getName() + " - " + orderdWaypoints.get(1).getName();
                AStarResult result = AStarAlgorithm.search(graph, start, end, segmentName, w, limit, Application.lsiWeights, weightedLsi, visitedEdges, reduceDoubleEdges);
                FileWriter fstream = new FileWriter(new File(name + "_Exp.txt"));
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(DORENDABuilder.build(result, graph));
                out.close();
                fstream.close();

                result.print();
                PathSegment segment = result.getPath();
                segment.setStartWaypoint(orderdWaypoints.get(0));
                segment.setEndWaypoint(orderdWaypoints.get(1));
                globalLength += result.getLength();
                path.addSegment(segment);
                System.out.println(result.getPath().getLength());
            } catch (Exception e) {
                System.out.println("Error running A-Star: " + e.toString());
                return;
            }
        }
        time_astar = (System.currentTimeMillis() - time_astar) / 1000;
        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println("Summary");
        System.out.println();
        System.out.println("Name: " + name);
        System.out.println("Waypoints: " + waypoints.size());
        System.out.println("Graphname: " + graph.getName());
        System.out.println();
        System.out.println("Times:");
        System.out.println("Loading Waypoints: " + time_waypoints);
        if (waypoints.size() > 2) System.out.println("TSP: " + time_tsp);
        System.out.println("Finding Closest Points: " + time_nearest);
        System.out.println("A-Star: " + time_astar);
        System.out.println();
        System.out.println("Global Length: " + distance.distanceLinestring(path.getGeoLineString(false)) + " Meter");
        System.out.println("Global Length (including Waypoints): " + distance.distanceLinestring(path.getGeoLineString(true)) + " Meter");
        System.out.println("--------------------------------------------------");
        System.out.println();

        try {
            FileWriter fstream = new FileWriter(new File(name + "_Topo.GPX"));
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(GPXBuilder.build(path.getTopoLineString(), path.getName()));
            out.close();
            fstream.close();
            fstream = new FileWriter(new File(name + "_Topo.txt"));
            out = new BufferedWriter(fstream);
            out.write(DORENDABuilder.build(path.getTopoLineString()));
            out.close();
            fstream.close();
            fstream = new FileWriter(new File(name + "_Geo.gpx"));
            out = new BufferedWriter(fstream);
            out.write(GPXBuilder.build(path.getGeoLineString(true), path.getName()));
            out.close();
            fstream.close();
            fstream = new FileWriter(new File(name + "_Geo.txt"));
            out = new BufferedWriter(fstream);
            out.write(DORENDABuilder.build(path));
            out.close();
            fstream.close();
        } catch (IOException e) {
            System.out.println("Error writing GPX file");
        }
    }

    /**
     * Gibt das Benchmarks menue aus
     */
    public static void benchmarkMenu() {
        List<Position> places = new ArrayList<Position>();
        places.add(new Position(-10.99845000, 49.39349500));
        places.add(new Position(-11.18493167, 49.49496333));
        boolean quit = false;
        do {
            System.out.println("==================================================");
            System.out.println("Benchmarks");
            System.out.println("==================================================");
            System.out.println();
            System.out.println("Please Make a selection:");
            System.out.println("[1] Weighted A-Star");
            System.out.println("[2] A-Star with limited OpenList");
            System.out.println("[3] TSP Methodes");
            System.out.println("[4] Genetic TSP Generations");
            System.out.println("[0] exit");

            System.out.print("Selection: ");
            int menu = scan.nextInt();
            switch (menu) {
                case 1:
                    System.out.print("Start W: ");
                    double wStart = scan.nextDouble();
                    System.out.print("End W: ");
                    double wEnd = scan.nextDouble();
                    System.out.print("Steps: ");
                    double wStep = scan.nextDouble();
                    compareAStarW(places, wStart, wEnd, wStep);
                    break;
                case 2:
                    System.out.print("Start Limit: ");
                    int lStart = scan.nextInt();
                    System.out.print("End Limit: ");
                    int lEnd = scan.nextInt();
                    System.out.print("Steps: ");
                    int lStep = scan.nextInt();
                    compareAStarLimitedOpen(places, lStart, lEnd, lStep);
                    break;
                case 3:
                    System.out.print("Number of Points: ");
                    int points = scan.nextInt();
                    compareTSP(points);
                    break;
                case 4:
                    System.out.print("Number of Points: ");
                    int gpoints = scan.nextInt();
                    System.out.print("Start Generations: ");
                    int gStart = scan.nextInt();
                    System.out.print("End Generations: ");
                    int gEnd = scan.nextInt();
                    System.out.print("Steps: ");
                    int gStep = scan.nextInt();
                    compareGeneticGens(gpoints, gStart, gEnd, gStep);
                    break;
                case 0:
                    quit = true;
                    break;
                default:
                    System.out.println("Invalid Entry!");
            }
        }
        while (!quit);
    }

    /**
     * Vergleicht A-Stern mit einem bereich von OverDo Faktoren
     *
     * @param places Testpunke fuer den Benchmark
     * @param start  Statr OverDo Faktor
     * @param end    End OverDo Faktor
     * @param step   Inkrementschritte
     */
    public static void compareAStarW(List<Position> places, double start, double end, double step) {
        if (end < start) {
            double tmp = end;
            end = start;
            start = tmp;
        }
        Node startNode = graph.findClosest(places.get(0), false);
        Node endNode = graph.findClosest(places.get((1)), false);
        System.out.println("==================================================");
        System.out.println("Benchmark A-Star with weighted h");
        System.out.println("From " + start + " to " + end + " steps " + step);
        System.out.println(distance.calcDist(startNode, endNode));
        System.out.println("==================================================");
        System.out.println();

        // Vergleichs A-Stern mit w = 1
        AStarResult compareResult;
        try {
            compareResult = AStarAlgorithm.search(graph, startNode, endNode, "Compare", 1);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        //
        double curr;
        for (curr = start; curr <= end; curr += step) {
            Path path = new Path("");
            AStarResult result;
            try {
                result = AStarAlgorithm.search(graph, startNode, endNode, "Compare " + curr, curr);
                path.addSegment(result.getPath());
                FileWriter fstream = new FileWriter(curr + ".gpx");
                BufferedWriter out = new BufferedWriter(fstream);
                String gpx = GPXBuilder.build(path.getGeoLineString(true), path.getName());
                out.write(gpx);
                out.close();
                fstream.close();
                System.out.println("Result for w=" + curr);
                System.out.println("Expanded Nodes: " + result.getExpandedNodesCount() + " (" + (result.getExpandedNodesCount() - compareResult.getExpandedNodesCount()) + ")");
                System.out.println("Path Length: " + result.getPath().getLength() + " (" + (result.getPath().getLength() - compareResult.getPath().getLength()) + ")");
                System.out.println("SortTime: " + result.getSorttime() + " (" + (result.getSorttime() - compareResult.getSorttime()) + ")");
                System.out.println("Runtime: " + result.getRuntime() + " (" + (result.getRuntime() - compareResult.getRuntime()) + ")");
                System.out.println();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Vergleicht A-Stern mit einem bereich von Limits fuer die Open Liste
     *
     * @param places Testpunke fuer den Benchmark
     * @param start  Statr Limit
     * @param end    End Limit
     * @param step   Inkrementschritte
     */
    public static void compareAStarLimitedOpen(List<Position> places, int start, int end, int step) {
        if (end < start) {
            int tmp = end;
            end = start;
            start = tmp;
        }
        Node startNode = graph.findClosest(places.get(0), false);
        Node endNode = graph.findClosest(places.get((1)), false);
        System.out.println("==================================================");
        System.out.println("Benchmark A-Star with weighted h");
        System.out.println("From " + start + " to " + end + " steps " + step);
        System.out.println(distance.calcDist(startNode, endNode));
        System.out.println("==================================================");
        System.out.println();

        AStarResult compareResult;
        try {
            compareResult = AStarAlgorithm.search(graph, startNode, endNode, "Compare", 1, 0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        //
        int curr;
        for (curr = start; curr <= end; curr += step) {
            Path path = new Path("");
            AStarResult result;
            try {
                result = AStarAlgorithm.search(graph, startNode, endNode, "Compare " + curr, 1, curr);
                path.addSegment(result.getPath());
                FileWriter fstream = new FileWriter(curr + ".gpx");
                BufferedWriter out = new BufferedWriter(fstream);
                String gpx = GPXBuilder.build(path.getGeoLineString(true), path.getName());
                out.write(gpx);
                out.close();
                fstream.close();
                System.out.println("Result for w=" + curr);
                System.out.println("Expanded Nodes: " + result.getExpandedNodesCount() + " (" + (result.getExpandedNodesCount() - compareResult.getExpandedNodesCount()) + ")");
                System.out.println("Path Length: " + result.getPath().getLength() + " (" + (result.getPath().getLength() - compareResult.getPath().getLength()) + ")");
                System.out.println("SortTime: " + result.getSorttime() + " (" + (result.getSorttime() - compareResult.getSorttime()) + ")");
                System.out.println("Runtime: " + result.getRuntime() + " (" + (result.getRuntime() - compareResult.getRuntime()) + ")");
                System.out.println();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Vergleicht TSP Methoden
     *
     * @param points anzahl der Punkte
     */
    public static void compareTSP(int points) {
        List<Waypoint> waypoints;
        List<Waypoint> orderdWaypoints;
        System.out.println("==================================================");
        System.out.println("TSP Compare");
        System.out.println("Number of Waypoints: " + points);
        System.out.println("==================================================");
        System.out.println();
        waypoints = random.randomWaypoints(points);
        for (TSP.tspType currTSP : TSP.tspType.values()) {
            double time_tsp = System.currentTimeMillis();
            System.out.println("Running " + currTSP + " with " + (points) + " Points");
            try {
                orderdWaypoints = TSP.sort(waypoints, currTSP);
                time_tsp = (System.currentTimeMillis() - time_tsp) / 1000;
                FileWriter fstream;
                try {
                    fstream = new FileWriter(currTSP.toString() + ".GPX");
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(GPXBuilder.build(orderdWaypoints, currTSP.toString()));
                    out.close();
                } catch (IOException e) {
                    System.out.println("Error writing GPX File");
                }
                System.out.println("Path length: " + distance.distanceList(orderdWaypoints));
                System.out.println(currTSP + " took " + time_tsp + " Secs");
                System.out.println();
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                System.out.println();
            }
        }
        System.out.println();
    }

    /**
     * Vergleicht den Genetischen Algo. fuer den TSP mit einem Intervall von Generationen
     *
     * @param points Testpunke fuer den Benchmark
     * @param start  Start Generationen Anzahl
     * @param end    End Generationen Anzahl
     * @param step   Inkrementschritte
     */
    public static void compareGeneticGens(int points, int start, int end, int step) {
        List<Waypoint> waypoints;
        List<Waypoint> orderdWaypoints;
        System.out.println("==================================================");
        System.out.println("Genetic TSP Generation Compare");
        System.out.println("Number of Waypoints: " + points);
        System.out.println("From " + start + " to " + end + " steps " + step);
        System.out.println("==================================================");
        System.out.println();
        waypoints = random.randomWaypoints(points);
        for (int curr = start; curr < end; curr += step) {
            System.out.println("Running TSP with " + curr + " Generations");
            try {
                double time_tsp = System.currentTimeMillis();
                orderdWaypoints = TSP.geneticAlgoPara(waypoints, 25, curr, 0);
                time_tsp = (System.currentTimeMillis() - time_tsp) / 1000;
                FileWriter fstream;
                try {
                    fstream = new FileWriter("Genetic_" + curr + ".GPX");
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(GPXBuilder.build(orderdWaypoints, "Genetic_" + curr));
                    out.close();
                } catch (IOException e) {
                    System.out.println("Error writing GPX File");
                }
                System.out.println("Path length: " + distance.distanceList(orderdWaypoints));
                System.out.println("Generations:" + curr + " took " + time_tsp + " Secs");
                System.out.println();
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                System.out.println();
            }
        }
        System.out.println();
    }

    /**
     * Graph Menue
     */
    public static void graphMenu() {
        boolean quit = false;
        do {
            System.out.println("==================================================");
            System.out.println("Graph");
            System.out.println("==================================================");
            System.out.println();
            System.out.println("Please Make a selection:");
            System.out.println("[1] Graph Informationen");
            System.out.println("[2] Dump Graph (Dorenda)");
            System.out.println("[3] Load Graph from File");
            System.out.println("[4] Load Graph from DB");
            System.out.println("[0] exit");

            System.out.print("Selection: ");
            int menu = scan.nextInt();
            String name, file;
            switch (menu) {
                case 1:
                    graphInformation();
                    break;
                case 2:
                    System.out.print("Filename: ");
                    file = scan.next();
                    try {
                        dumpString(file, DORENDABuilder.build(graph, true, false));
                    } catch (IOException e) {
                        System.out.println("Error while writing File (" + e.getMessage() + ")");
                    }
                    break;
                case 3:
                    System.out.print("Graphname: ");
                    name = scan.next();
                    loadGraph_File(name);
                    break;
                case 4:
                    System.out.print("Graphname: ");
                    name = scan.next();
                    System.out.print("BoundingBoxFile: ");
                    file = scan.next();
                    loadGraph_Database(file, name);
                    break;
                case 0:
                    quit = true;
                    break;
                default:
                    System.out.println("Invalid Entry!");
            }
        }
        while (!quit);
    }

    /**
     * Ausgabe der Graphinformationen
     */
    public static void graphInformation() {
        System.out.println("==================================================");
        System.out.println("Graph Information");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Name: " + graph.getName());
        System.out.println("Date: " + graph.getGenDateString());
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
        System.out.println("Geometrys: " + graph.getGeometryCount());
        System.out.println("End Points: " + graph.getGeometryCount());
        try {
            File file = new File(graph.getFileName());
            System.out.println("File Size: " + file.length() + "Byte");
        } catch (Exception e) {
            System.out.println("Error while getting File Information (" + e.getMessage() + ")");
        }
    }

    /**
     * Laedt den Graph aus einer Datei
     *
     * @param filename Dateiname
     */
    public static void loadGraph_File(String filename) {
        // Graph aus Datei laden
        Application.graph = null;
        double time_graph = System.currentTimeMillis();
        filename = filename + ".DAT";
        Application.println("============================================================");
        Application.println("Fetching Data from " + filename + " and building graph");
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream obj_in = new ObjectInputStream(fis);
            Object obj = obj_in.readObject();
            if (obj instanceof NavGraph) {
                Application.graph = (NavGraph) obj;
            } else {
                throw new Exception("Saved Data != NavGraph");
            }
            obj_in.close();
            fis.close();
            time_graph = (System.currentTimeMillis() - time_graph) / 1000;
            double time_init = System.currentTimeMillis();
            Application.println("Init Graph");
            graph.initGraph();
            time_init = (System.currentTimeMillis() - time_init) / 1000;
            Application.println("Init Graph took " + time_init + " Secs");
            Application.println("Fetching Data and building graph took: " + time_graph + "sec");
            Application.println("============================================================");
        } catch (Exception e) {
            Application.println("Error: " + e.toString());
        }
    }

    /**
     * Laedt den Graph von der Datenbank
     *
     * @param bBoxFile Dateiname der Datei in der die Bereichskoordinaten
     * @param name     Name des Graphen
     */
    public static void loadGraph_Database(String bBoxFile, String name) {
        Application.graph = null;
        Position[] boundingBox;
        try {
            boundingBox = CSV.readPositionArray(bBoxFile);
            loadGraph_Database(boundingBox, name);
        } catch (Exception e) {
            System.out.println("Error while reading File");
        }
    }

    /**
     * Laedt den Graph von der Datenbank
     *
     * @param bBox Array der Bereichskoordinaten
     * @param name Graphname
     */
    public static void loadGraph_Database(Position[] bBox, String name) {
        // Graph aus Datei laden
        double time_graph = System.currentTimeMillis();
        System.out.println("============================================================");
        Application.connection = new geoDatabaseConnection(dbhost, dbport, dbuser, dbpasswd, dbname);

        // Graph generieren
        File file = new File(name + ".DAT");
        // Graph von DB laden
        Application.println("Fetching Data from DB and building graph");
        try {
            Application.connection.connect();
            graph = Application.connection.getGraph(bBox, name);
            FileOutputStream fos = new FileOutputStream(file.getName());
            ObjectOutputStream out2 = new ObjectOutputStream(fos);
            out2.writeObject(graph);
            out2.close();
            fos.close();
            time_graph = (System.currentTimeMillis() - time_graph) / 1000;
            Application.println("Fetching Data and building graph took: " + time_graph + "sec");
            double time_init = System.currentTimeMillis();
            Application.println("Init Graph");
            graph.initGraph();
            time_init = (System.currentTimeMillis() - time_init) / 1000;
            Application.println("Init Graph took " + time_init + " Secs");
            Application.println("============================================================");
        } catch (Exception e) {
            System.out.println("Error: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Gibt einen String aus sollte das Programm im Verbose Modus laufen
     *
     * @param line Der auszugebende String
     */
    public static void println(String line) {
        if (verbose) System.out.println(line);
    }

    /**
     * Scripting Menue
     * @throws Exception IO Fehler
     */
    public static void scriptMenu() throws Exception{
        boolean quit = false;
        do {
            System.out.println("==================================================");
            System.out.println("Scripting Menu");
            System.out.println("==================================================");
            System.out.println();
            System.out.println("Please Make a selection:");
            System.out.println("[1] Testaufgabe Bamberg");
            System.out.println("[2] Testaufgabe Wuerzburg");
            System.out.println("[3] Testaufgabe Nuernberg");
            System.out.println("[4] Load Script from File");
            System.out.println("[0] exit");

            System.out.print("Selection: ");
            int menu = scan.nextInt();
            String name, file;
            switch (menu) {
                case 1:
                    loadScript("Bamberg.foo");
                    break;
                case 2:
                    loadScript("Wuerzburg.foo");
                    break;
                case 3:
                    loadScript("Nuernberg.foo");
                    break;
                case 4:
                    System.out.print("Filename: ");
                    name = scan.next();
                    loadScript(name);
                    break;
                case 0:
                    quit = true;
                    break;
                default:
                    System.out.println("Invalid Entry!");
            }
        }
        while (!quit);
    }

    /**
     * Bearbeitet ein "Script"
     *
     * @param filename Dateiname fuer das Script
     * @throws Exception IO Fehler
     */
    public static void loadScript(String filename) throws Exception {
        File scriptFile = new File(filename);
        if (!scriptFile.exists()) {
            System.out.println("Scriptfile not found");
            return;
        }
        FileInputStream fis = new FileInputStream(scriptFile);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
        String strLine, tokens[];
        while ((strLine = in.readLine()) != null) {
            tokens = strLine.split(" ", 2);
            if (tokens[0].toUpperCase().equals("PRINT")) {
                System.out.println(tokens[1]);
            } else if (tokens[0].toUpperCase().equals("LOADFILE")) {
                if (!Application.graph.getName().toLowerCase().equals(tokens[1].toLowerCase())) {

                File file = new File(tokens[1] + ".DAT");
                if (file.exists()) {
                    Application.loadGraph_File(tokens[1]);
                }
                }
            } else if (tokens[0].toUpperCase().equals("PATH")) {
                String pathPara[] = tokens[1].split(" ");
                boolean reduce = true;
                boolean lsi = true;
                if (pathPara[4].equals("0")) {
                    reduce = false;
                }
                if (pathPara[5].equals("0")) {
                    lsi = false;
                }
                findPath(pathPara[0], pathPara[1], Double.parseDouble(pathPara[2]), Integer.parseInt(pathPara[3]),  true, reduce, lsi);
            } else if (tokens[0].equals("//")) {

            } else if (tokens[0].toUpperCase().equals("DORENDA")) {
                String command = "java -jar dorendaclient.jar -h geo.informatik.fh-nuernberg.de -p 8088 -c pp.dorenda.client.UniversalPainter -a " + tokens[1] + ";c";
                Runtime.getRuntime().exec(command);
            } else {
                System.out.println("Unknown Command: " + tokens[0]);
            }

        }
    }

    /**
     * Schreibt einen String in eine Datei
     * @param filename Der Dateiname
     * @param s Der zu schreibende String
     * @throws IOException Fehler beim schreiben der Datei
     */
    public static void dumpString(String filename, String s) throws IOException {
        FileWriter fstream = new FileWriter(filename);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(s);
        out.close();
    }

}

