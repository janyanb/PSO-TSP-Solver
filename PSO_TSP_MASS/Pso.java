package edu.uwb.css534;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import edu.uw.bothell.css.dsl.MASS.MASS;
import java.util.ArrayList;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.*;

public class Pso {
    public static void main(String[] args) {
        // MASS Initialization
        MASS.setLoggingLevel(LogLevel.ERROR);
        MASS.init();

        int numCities = 36;
        int numParticles = 100; // Population size
        double[][] coordinates = new double[numCities][2];
        double[][] distanceMatrix = new double[numCities][numCities];

        // Read City coordinates from cities.txt
        String filename = "cities.txt";
        List<String> cityNames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                int index = 0;

                while ((line = reader.readLine()) != null && index < numCities) {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 3) {
                        System.err.println("Invalid line format: " + line);
                        continue;
                    }

                    String cityName = parts[0];
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);

                    coordinates[index][0] = x;
                    coordinates[index][1] = y;

                    cityNames.add(cityName);
                    index++;
                }

                System.out.println("Coordinates loaded successfully");
            } catch (IOException e) {
            System.err.println("Error reading city coordinates: " + e.getMessage());
            MASS.finish();
            return;
        }
 // Calculate the distance matrix
 for (int i = 0; i < numCities; i++) {
    for (int j = 0; j < numCities; j++) {
        double dx = coordinates[i][0] - coordinates[j][0];
        double dy = coordinates[i][1] - coordinates[j][1];
        distanceMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
    }
}
System.out.println("Distance Matrix computed");

try {
        // Create 36 Places for Cities
    Places cities = new Places(1, City.class.getName(), null, numCities);
    System.out.println("Places for Cities created successfully.");

    // Assign City names and coordinates to Places
    for (int i = 0; i < numCities; i++) {
        Object[] cityArgs = new Object[]{cityNames.get(i), i, coordinates[i][0], coordinates[i][1]};
        cities.callAll(City.init, cityArgs); // Use the static int init directly
    }
    // Create 100 agents for particle population
    Agents particles = new Agents(1, Particle.class.getName(), numCities, cities, numParticles);
    System.out.println("Particles created successfully.");

    // Holds the global best route and fitness
    double globalBestFitness = Double.MAX_VALUE;
    List<Character> globalBestRoute = null;

    for (int iter = 0; iter < 1000; iter++) { // Number of iterations
        // Update particles and find local best
        particles.callAll(Particle.CALCULATE_FITNESS, distanceMatrix);
        particles.callAll(Particle.UPDATE_PBEST, distanceMatrix);

        // Synchronize global best
        Object localBestResults = particles.callAll(Particle.GET_LOCAL_BEST, null);

        for (Object result : (Object[]) localBestResults) {
            Object[] localBest = (Object[]) result;
            double localBestFitness = (double) localBest[0];
            List<Character> localBestRoute = (List<Character>) localBest[1];

            if (localBestFitness < globalBestFitness) {
                globalBestFitness = localBestFitness;
                globalBestRoute = new ArrayList<>(localBestRoute);
            }
        }

        char[] globalBestRouteArray = new char[globalBestRoute.size()];
        for (int i = 0; i < globalBestRoute.size(); i++) {
            globalBestRouteArray[i] = globalBestRoute.get(i);
        }

        particles.callAll(Particle.UPDATE_PARTICLES, new Object[]{globalBestRouteArray, 0.1, 0.2}); // C1, C2
    }

    System.out.println("Global Best Fitness: " + globalBestFitness);
    System.out.println("Global Best Route: " + globalBestRoute);
    MASS.finish();

} catch (Exception e) {
    System.err.println("Error in PSO execution: " + e.getMessage());
    e.printStackTrace();
    MASS.finish();
}
}
}
