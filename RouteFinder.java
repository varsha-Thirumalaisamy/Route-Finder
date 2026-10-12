import java.util.*;

/**
 * Intelligent Route Finder
 * 
 * An interactive Java console application that finds the shortest or fastest
 * route between Indian cities using Dijkstra's Algorithm and the A* Algorithm.
 * 
 * Features:
 * - Weighted graph with distance (km) and travel time (minutes).
 * - Haversine straight-line distance formula for geographic heuristics.
 * - Admissible heuristic for time optimization based on maximum network speed.
 * - Comparative performance analysis (Route, Distance, Time, Nodes Explored).
 * - User input validation and interactive console loop.
 */
public class RouteFinder {

    // City geographic coordinates: Map<CityName, [Latitude, Longitude]>
    private static final Map<String, double[]> coordinates = new HashMap<>();

    // Graph adjacency list: Map<CityName, List<Edge>>
    private static final Map<String, List<Edge>> graph = new HashMap<>();

    // Maximum speed on any road in the network (in km/h)
    private static double maximumSpeed = 0.0;

    /**
     * Edge represents a road connection between cities.
     */
    static class Edge {
        String targetCity;
        double distanceKm;
        double travelTimeMin;
        double speedKmH;

        public Edge(String targetCity, double distanceKm, double speedKmH) {
            this.targetCity = targetCity;
            this.distanceKm = distanceKm;
            this.speedKmH = speedKmH;
            // Travel time in minutes = (distance in km / speed in km/h) * 60
            this.travelTimeMin = (distanceKm / speedKmH) * 60.0;
        }
    }

    /**
     * Represents the outcome of running a pathfinding algorithm.
     */
    static class SearchResult {
        String algorithmName;
        List<String> path;
        double totalDistanceKm;
        double totalTimeMin;
        int nodesExplored;
        boolean routeFound;

        public SearchResult(String algorithmName) {
            this.algorithmName = algorithmName;
            this.path = new ArrayList<>();
            this.totalDistanceKm = 0.0;
            this.totalTimeMin = 0.0;
            this.nodesExplored = 0;
            this.routeFound = false;
        }
    }

    /**
     * Helper node for Priority Queue in Dijkstra and A* search.
     */
    static class QueueNode implements Comparable<QueueNode> {
        String city;
        double priorityCost; // For Dijkstra: g-cost. For A*: f-cost = g + h.
        double costSoFar;     // Actual cost accumulated from start (g-cost)

        public QueueNode(String city, double priorityCost, double costSoFar) {
            this.city = city;
            this.priorityCost = priorityCost;
            this.costSoFar = costSoFar;
        }

        @Override
        public int compareTo(QueueNode other) {
            return Double.compare(this.priorityCost, other.priorityCost);
        }
    }

    /**
     * Initializes coordinates of all 13 cities and road connections.
     */
    private static void initializeMap() {
        // 1. City coordinates [Latitude, Longitude]
        coordinates.put("Delhi", new double[]{28.6139, 77.2090});
        coordinates.put("Jaipur", new double[]{26.9124, 75.7873});
        coordinates.put("Agra", new double[]{27.1767, 78.0081});
        coordinates.put("Lucknow", new double[]{26.8467, 80.9462});
        coordinates.put("Ahmedabad", new double[]{23.0225, 72.5714});
        coordinates.put("Indore", new double[]{22.7196, 75.8577});
        coordinates.put("Bhopal", new double[]{23.2599, 77.4126});
        coordinates.put("Mumbai", new double[]{19.0760, 72.8777});
        coordinates.put("Pune", new double[]{18.5204, 73.8567});
        coordinates.put("Hyderabad", new double[]{17.3850, 78.4867});
        coordinates.put("Bengaluru", new double[]{12.9716, 77.5946});
        coordinates.put("Chennai", new double[]{13.0827, 80.2707});
        coordinates.put("Nagpur", new double[]{21.1458, 79.0882});

        // Initialize adjacency list for all cities
        for (String city : coordinates.keySet()) {
            graph.put(city, new ArrayList<>());
        }

        // 2. Add bidirectional roads
        addRoad("Delhi", "Jaipur", 280, 65);
        addRoad("Delhi", "Agra", 233, 70);
        addRoad("Agra", "Lucknow", 335, 65);
        addRoad("Delhi", "Lucknow", 555, 60);
        addRoad("Jaipur", "Ahmedabad", 660, 60);
        addRoad("Jaipur", "Indore", 600, 55);
        addRoad("Agra", "Bhopal", 570, 55);
        addRoad("Ahmedabad", "Mumbai", 525, 70);
        addRoad("Ahmedabad", "Indore", 395, 60);
        addRoad("Indore", "Bhopal", 195, 60);
        addRoad("Indore", "Mumbai", 590, 60);
        addRoad("Bhopal", "Nagpur", 350, 55);
        addRoad("Mumbai", "Pune", 150, 65);
        addRoad("Pune", "Hyderabad", 560, 65);
        addRoad("Nagpur", "Hyderabad", 500, 65);
        addRoad("Hyderabad", "Bengaluru", 570, 70);
        addRoad("Pune", "Bengaluru", 840, 65);
        addRoad("Bengaluru", "Chennai", 345, 70);
        addRoad("Hyderabad", "Chennai", 625, 65);
        addRoad("Lucknow", "Nagpur", 1030, 55);
    }

    /**
     * Adds an undirected (two-way) road between two cities and tracks max speed.
     */
    private static void addRoad(String city1, String city2, double distanceKm, double speedKmH) {
        graph.get(city1).add(new Edge(city2, distanceKm, speedKmH));
        graph.get(city2).add(new Edge(city1, distanceKm, speedKmH));

        if (speedKmH > maximumSpeed) {
            maximumSpeed = speedKmH;
        }
    }

    /**
     * Computes the great-circle straight-line distance between two coordinates
     * using the Haversine formula.
     *
     * @return Straight-line distance in kilometers
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates the heuristic estimate h(n) from currentCity to destinationCity.
     * 
     * - Distance mode: straight-line distance (km) using Haversine.
     * - Time mode: straightLineDistance / maximumSpeed * 60 (minutes).
     */
    public static double calculateHeuristic(String currentCity, String destinationCity, String mode) {
        double[] coord1 = coordinates.get(currentCity);
        double[] coord2 = coordinates.get(destinationCity);

        double straightLineDistKm = haversine(coord1[0], coord1[1], coord2[0], coord2[1]);

        if ("time".equalsIgnoreCase(mode)) {
            // Heuristic time in minutes = (straightLineDistance / maximumSpeed) * 60
            return (straightLineDistKm / maximumSpeed) * 60.0;
        } else {
            // Heuristic distance in km
            return straightLineDistKm;
        }
    }

    /**
     * Dijkstra's Algorithm
     * Explores nodes ordered solely by accumulated cost from the start node g(n).
     */
    public static SearchResult findRouteDijkstra(String startCity, String destinationCity, String mode) {
        SearchResult result = new SearchResult("Dijkstra's Algorithm");

        // Tracks minimum cost found so far to reach each city
        Map<String, Double> costSoFar = new HashMap<>();
        // Tracks predecessor for route reconstruction
        Map<String, String> parentCity = new HashMap<>();
        // Tracks the road edge used to reach each city
        Map<String, Edge> parentEdge = new HashMap<>();
        // Set of settled / visited nodes
        Set<String> visited = new HashSet<>();

        PriorityQueue<QueueNode> pq = new PriorityQueue<>();

        // Initialize start node
        costSoFar.put(startCity, 0.0);
        pq.add(new QueueNode(startCity, 0.0, 0.0));

        while (!pq.isEmpty()) {
            QueueNode current = pq.poll();
            String currCity = current.city;

            // If already settled, skip
            if (visited.contains(currCity)) {
                continue;
            }

            // Mark node as visited and count as explored
            visited.add(currCity);
            result.nodesExplored++;

            // If destination is reached, stop search
            if (currCity.equalsIgnoreCase(destinationCity)) {
                result.routeFound = true;
                break;
            }

            // Inspect all neighboring roads
            for (Edge edge : graph.get(currCity)) {
                String neighbor = edge.targetCity;
                if (visited.contains(neighbor)) {
                    continue;
                }

                // Cost increment depending on optimization mode
                double edgeCost = "time".equalsIgnoreCase(mode) ? edge.travelTimeMin : edge.distanceKm;
                double newCost = costSoFar.get(currCity) + edgeCost;

                if (newCost < costSoFar.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    costSoFar.put(neighbor, newCost);
                    parentCity.put(neighbor, currCity);
                    parentEdge.put(neighbor, edge);
                    // Dijkstra uses priority = g(n)
                    pq.add(new QueueNode(neighbor, newCost, newCost));
                }
            }
        }

        if (result.routeFound) {
            reconstructPath(result, startCity, destinationCity, parentCity, parentEdge);
        }

        return result;
    }

    /**
     * A* Algorithm
     * Explores nodes ordered by f(n) = g(n) + h(n), where:
     * - g(n) is the exact cost from start to node n.
     * - h(n) is the admissible heuristic estimate from node n to destination.
     */
    public static SearchResult findRouteAStar(String startCity, String destinationCity, String mode) {
        SearchResult result = new SearchResult("A* Algorithm");

        // Exact accumulated cost g(n) from start to each city
        Map<String, Double> gCost = new HashMap<>();
        // Predecessor map
        Map<String, String> parentCity = new HashMap<>();
        // Edge used to reach each city
        Map<String, Edge> parentEdge = new HashMap<>();
        // Set of visited nodes
        Set<String> visited = new HashSet<>();

        PriorityQueue<QueueNode> pq = new PriorityQueue<>();

        // Initialize start node
        gCost.put(startCity, 0.0);
        double initialH = calculateHeuristic(startCity, destinationCity, mode);
        pq.add(new QueueNode(startCity, initialH, 0.0));

        while (!pq.isEmpty()) {
            QueueNode current = pq.poll();
            String currCity = current.city;

            // If already settled, skip
            if (visited.contains(currCity)) {
                continue;
            }

            // Mark node as visited and count as explored
            visited.add(currCity);
            result.nodesExplored++;

            // Destination reached
            if (currCity.equalsIgnoreCase(destinationCity)) {
                result.routeFound = true;
                break;
            }

            // Inspect all neighboring roads
            for (Edge edge : graph.get(currCity)) {
                String neighbor = edge.targetCity;
                if (visited.contains(neighbor)) {
                    continue;
                }

                double edgeCost = "time".equalsIgnoreCase(mode) ? edge.travelTimeMin : edge.distanceKm;
                double tentativeG = gCost.get(currCity) + edgeCost;

                if (tentativeG < gCost.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gCost.put(neighbor, tentativeG);
                    parentCity.put(neighbor, currCity);
                    parentEdge.put(neighbor, edge);

                    // A* priority = f(n) = g(n) + h(n)
                    double h = calculateHeuristic(neighbor, destinationCity, mode);
                    double fCost = tentativeG + h;
                    pq.add(new QueueNode(neighbor, fCost, tentativeG));
                }
            }
        }

        if (result.routeFound) {
            reconstructPath(result, startCity, destinationCity, parentCity, parentEdge);
        }

        return result;
    }

    /**
     * Reconstructs the complete route and computes total distance & travel time.
     */
    private static void reconstructPath(SearchResult result, String startCity, String destinationCity,
                                        Map<String, String> parentCity, Map<String, Edge> parentEdge) {
        LinkedList<String> path = new LinkedList<>();
        String current = destinationCity;

        double totalDist = 0.0;
        double totalTime = 0.0;

        while (current != null && !current.equalsIgnoreCase(startCity)) {
            path.addFirst(current);
            Edge edge = parentEdge.get(current);
            if (edge != null) {
                totalDist += edge.distanceKm;
                totalTime += edge.travelTimeMin;
            }
            current = parentCity.get(current);
        }

        if (current != null) {
            path.addFirst(current);
        }

        result.path = path;
        result.totalDistanceKm = totalDist;
        result.totalTimeMin = totalTime;
    }

    /**
     * Converts minutes into hours and minutes display format.
     */
    private static String formatTime(double minutes) {
        long totalMins = Math.round(minutes);
        long hours = totalMins / 60;
        long remainingMins = totalMins % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%.2f mins (%d hrs %d mins)", minutes, hours, remainingMins);
        } else {
            return String.format(Locale.US, "%.2f mins (%d mins)", minutes, remainingMins);
        }
    }

    /**
     * Sanitizes user input by stripping non-alphanumeric characters (including BOM and control chars)
     * and leading/trailing whitespace.
     */
    private static String cleanInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9 ]", "").trim();
    }

    /**
     * Finds the canonical city name ignoring case and leading/trailing whitespace.
     */
    private static String resolveCityName(String input) {
        String cleaned = cleanInput(input);
        if (cleaned.isEmpty()) return null;
        for (String city : coordinates.keySet()) {
            if (city.equalsIgnoreCase(cleaned)) {
                return city;
            }
        }
        return null;
    }

    /**
     * Validates and resolves optimization mode ('distance' or 'time').
     */
    private static String resolveMode(String input) {
        String cleaned = cleanInput(input).toLowerCase();
        if (cleaned.isEmpty()) return null;
        if (cleaned.equals("distance") || cleaned.equals("d") || cleaned.equals("1")) {
            return "distance";
        }
        if (cleaned.equals("time") || cleaned.equals("t") || cleaned.equals("2")) {
            return "time";
        }
        return null;
    }

    /**
     * Displays formatted search results.
     */
    private static void displayResultCard(SearchResult result) {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("  " + result.algorithmName.toUpperCase());
        System.out.println("----------------------------------------------------------------------");
        if (!result.routeFound) {
            System.out.println("  [!] No valid route found.");
            System.out.println("  Nodes Explored: " + result.nodesExplored);
            return;
        }

        System.out.println("  Selected Route    : " + String.join(" -> ", result.path));
        System.out.printf(Locale.US, "  Total Distance    : %.2f km%n", result.totalDistanceKm);
        System.out.println("  Total Travel Time : " + formatTime(result.totalTimeMin));
        System.out.println("  Nodes Explored    : " + result.nodesExplored);
    }

    /**
     * Displays a side-by-side or comparative summary.
     */
    private static void displayComparison(SearchResult dijkstra, SearchResult aStar) {
        System.out.println("======================================================================");
        System.out.println("                   ALGORITHM COMPARISON SUMMARY                       ");
        System.out.println("======================================================================");
        System.out.printf(Locale.US, "%-25s | %-18s | %-18s%n", "Metric", "Dijkstra", "A*");
        System.out.println("----------------------------------------------------------------------");
        System.out.printf(Locale.US, "%-25s | %-18d | %-18d%n", "Nodes Explored", dijkstra.nodesExplored, aStar.nodesExplored);
        System.out.printf(Locale.US, "%-25s | %-18.2f | %-18.2f%n", "Total Distance (km)", dijkstra.totalDistanceKm, aStar.totalDistanceKm);
        System.out.printf(Locale.US, "%-25s | %-18.2f | %-18.2f%n", "Total Time (mins)", dijkstra.totalTimeMin, aStar.totalTimeMin);
        System.out.println("----------------------------------------------------------------------");

        if (dijkstra.nodesExplored > aStar.nodesExplored) {
            int savedNodes = dijkstra.nodesExplored - aStar.nodesExplored;
            double pct = ((double) savedNodes / dijkstra.nodesExplored) * 100.0;
            System.out.printf(Locale.US, ">> Key Takeaway: A* explored %d fewer node(s) (%.1f%% reduction) than Dijkstra%n",
                    savedNodes, pct);
            System.out.println("   because the Haversine heuristic guided A* directly towards the destination!");
        } else if (dijkstra.nodesExplored == aStar.nodesExplored) {
            System.out.println(">> Both algorithms explored the same number of nodes for this direct/short query.");
        } else {
            System.out.println(">> Both algorithms successfully found the optimal route.");
        }
        System.out.println("======================================================================\n");
    }

    /**
     * Prints available cities for the user.
     */
    private static void printAvailableCities() {
        System.out.println("\nAvailable Cities in Network (" + coordinates.size() + "):");
        List<String> sortedCities = new ArrayList<>(coordinates.keySet());
        Collections.sort(sortedCities);
        int col = 0;
        System.out.print("  ");
        for (String city : sortedCities) {
            System.out.printf("%-14s", city);
            col++;
            if (col % 4 == 0) {
                System.out.print("\n  ");
            }
        }
        System.out.println();
    }

    /**
     * Interactive console main loop.
     */
    public static void main(String[] args) {
        initializeMap();
        Scanner scanner = new Scanner(System.in);

        System.out.println("======================================================================");
        System.out.println("             INTELLIGENT ROUTE FINDER (DIJKSTRA vs A*)                ");
        System.out.println("======================================================================");
        System.out.println("  Find optimal paths between cities optimized by distance or travel time.");
        System.out.printf(Locale.US, "  Network: %d cities, %d road segments, Max speed: %.0f km/h%n",
                coordinates.size(), graph.values().stream().mapToInt(List::size).sum() / 2, maximumSpeed);

        boolean running = true;

        while (running) {
            printAvailableCities();

            // 1. Get and validate Start City
            String startCity = null;
            while (startCity == null) {
                System.out.print("\nEnter Start City: ");
                String input = scanner.nextLine();
                String cleaned = cleanInput(input);
                if (cleaned.isEmpty()) {
                    System.out.println("  [Error] City name cannot be empty. Please enter a city from the list.");
                    continue;
                }
                startCity = resolveCityName(cleaned);
                if (startCity == null) {
                    System.out.println("  [Error] City '" + cleaned + "' not found. Please choose from the list above.");
                }
            }

            // 2. Get and validate Destination City
            String destCity = null;
            while (destCity == null) {
                System.out.print("Enter Destination City: ");
                String input = scanner.nextLine();
                String cleaned = cleanInput(input);
                if (cleaned.isEmpty()) {
                    System.out.println("  [Error] Destination city cannot be empty. Please enter a city from the list.");
                    continue;
                }
                destCity = resolveCityName(cleaned);
                if (destCity == null) {
                    System.out.println("  [Error] City '" + cleaned + "' not found. Please choose from the list above.");
                } else if (destCity.equalsIgnoreCase(startCity)) {
                    System.out.println("  [Error] Destination cannot be the same as Start City (" + startCity + ").");
                    destCity = null;
                }
            }

            // 3. Get and validate Optimization Mode
            String mode = null;
            while (mode == null) {
                System.out.print("Choose Optimization Mode [distance / time]: ");
                String input = scanner.nextLine();
                mode = resolveMode(input);
                if (mode == null) {
                    System.out.println("  [Error] Invalid mode. Please enter 'distance' (or 'd') or 'time' (or 't').");
                }
            }

            System.out.println("\nSearching optimal route from " + startCity + " to " + destCity +
                    " (Optimizing for " + mode.toUpperCase() + ")...");

            // Run Dijkstra
            SearchResult dijkstraResult = findRouteDijkstra(startCity, destCity, mode);

            // Run A*
            SearchResult aStarResult = findRouteAStar(startCity, destCity, mode);

            // Display Results
            System.out.println("\n======================================================================");
            System.out.println("                           SEARCH RESULTS                             ");
            System.out.println("======================================================================");
            displayResultCard(dijkstraResult);
            displayResultCard(aStarResult);
            displayComparison(dijkstraResult, aStarResult);

            // Prompt to continue or exit
            System.out.print("Would you like to search for another route? (yes/no): ");
            String again = cleanInput(scanner.nextLine()).toLowerCase();
            if (!again.equals("y") && !again.equals("yes")) {
                running = false;
                System.out.println("\nThank you for using Intelligent Route Finder! Safe travels.");
            }
        }

        scanner.close();
    }
}
