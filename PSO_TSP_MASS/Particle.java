package edu.uwb.css534;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import edu.uw.bothell.css.dsl.MASS.Agent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class Particle extends Agent {
    private List<Character> route; 
    private List<Character> pbestRoute; 
    private double fitness;
    private double pbestFitness;

    public static final int CALCULATE_FITNESS = 0;
    public static final int UPDATE_PBEST = 1;
    public static final int GET_LOCAL_BEST = 2;
    public static final int UPDATE_PARTICLES = 3;

    public Particle(Object args) {
        // Initialize city candidates (A to Z and 0 to 9)
        String cityCandidates = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        route = new ArrayList<>();

        // Fill the route with characters
        for(char c: cityCandidates.toCharArray()){
            route.add(c);
        }

        // Shuffle list to create random route
        Collections.shuffle(route);

        // Initialize the personal best route to current route
        pbestRoute = new ArrayList<>(route);

        // Initialize fitness values
        fitness = Double.MAX_VALUE;
        pbestFitness = Double.MAX_VALUE;

        System.out.println("Initialized route: "+ route);
    }

    // Getter for route
    public List<Character> getRoute(){
        return route;
    }

    public Object callMethod(int method, Object args) {
        switch (method) {
        case CALCULATE_FITNESS:
            return calculateFitness((double[][]) args);
        case UPDATE_PBEST:
            updatePBest((double[][]) args);
            return null;
        case GET_LOCAL_BEST:
            return new Object[]{pbestFitness, pbestRoute};
        case UPDATE_PARTICLES:
            Object[] params = (Object[]) args;
            updateParticle((char[]) params[0], (double) params[1], (double) params[2]);
            return null;
            default:
            return null;
        }
    }

    public double calculateFitness(double[][] distanceMatrix) {
        String cityCandidates = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        double totalDistance = 0.0;
    
        for (int i = 0; i < route.size(); i++) {
            char currentCity = route.get(i);
            char nextCity = route.get((i + 1) % route.size()); // Wrap-around to the first city
    
            int from = cityCandidates.indexOf(currentCity);
            int to = cityCandidates.indexOf(nextCity);
            System.out.println("from: " + from + ", to: "+ to);
    
            // Debugging: Ensure indices are valid
            if (from == -1 || to == -1) {
                System.err.println("Error: City not found in cityCandidates.");
                System.err.println("currentCity: " + currentCity + ", nextCity: " + nextCity);
                continue; // Skip this iteration or handle the error
            }
    
            totalDistance += distanceMatrix[from][to];
        }
    
        fitness = totalDistance;
        return fitness;
    }

    // Updating personal best
    public void updatePBest(double[][] distanceMatrix) {
        double currentFitness = calculateFitness(distanceMatrix);
        if (currentFitness < pbestFitness) {
            pbestFitness = currentFitness;
            pbestRoute = new ArrayList<>(route);
        }
    }

    public void updateParticle(char[] globalBest, double C1, double C2) {
        route = updateRoute(route, pbestRoute, globalBest, C1, C2);
    }

    private static List<Character> updateRoute(List<Character> currentRoute, List<Character> pBest, char[] GBest, double C1, double C2) {
        List<Character> newRoute = new ArrayList<>(currentRoute.size());
        Set<Character> visited = new HashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Cognitive component
        if (random.nextDouble() < C1) {
            for (Character city : pBest) {
                if (random.nextDouble() < 0.5 && !visited.contains(city)) {
                    newRoute.add(city);
                    visited.add(city);
                }
            }
        }

        // Social component
        for (char city : GBest) {
            if (random.nextDouble() < C2 && !visited.contains(city)) {
                newRoute.add(city);
                visited.add(city);
            }
        }

        // Fill remaining cities from current route
        for (Character city : currentRoute) {
            if (!visited.contains(city)) {
                newRoute.add(city);
                visited.add(city);
            }
        }
 // Randomly swap two cities in the new route to introduce exploration
        int index1 = random.nextInt(newRoute.size());
        int index2 = random.nextInt(newRoute.size());
        Collections.swap(newRoute, index1, index2);

        return newRoute;
    }
}

