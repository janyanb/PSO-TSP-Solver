import mpi.MPI;
import mpi.MPIException;
import mpi.*;
import java.util.*;
import java.io.*;
import java.util.stream.IntStream;

public class PSOTSP_MPI {
    private static final int NUM_POINTS = 36;  // Number of cities
    private static final int POP_SIZE = 1000; // Population size
    private static final int MAX_ITER = 1000; // Maximum iterations
    private static final double C1 = 0.9;     // Cognitive component
    private static final double C2 = 0.9;     // Social component

    private static final String CITIES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static Random rng = new Random();

    public static void main(String[] args) throws MPIException {
        MPI.Init(args);
        long startTime = System.currentTimeMillis(); // Start time

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        List<double[]> cityCoordinates = readCityCoordinates("cities.txt", NUM_POINTS);
        double[][] distMatrix = computeDistanceMatrix(cityCoordinates);

        int localPopSize = POP_SIZE / size;

        char[][] localParticles = new char[localPopSize][NUM_POINTS];
        double[] localFitness = new double[localPopSize];
        Arrays.fill(localFitness, Double.MAX_VALUE);
        List<char[]> localPBest = new ArrayList<>();
        double[] localPBestFitness = new double[localPopSize];
        Arrays.fill(localPBestFitness, Double.MAX_VALUE);

        char[] globalGBestRoute = new char[NUM_POINTS];
        double[] globalGBestFitness = new double[]{Double.MAX_VALUE};

        if (rank == 0) {
        // Master rank initializes all routes
        char[][] allParticles = new char[POP_SIZE][NUM_POINTS];
        String cityCandidates = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        List<Character> charRoute = new ArrayList<>();
        for (char c : cityCandidates.toCharArray()) {
            charRoute.add(c);
        }

        for (int i = 0; i < POP_SIZE; i++) {
            Collections.shuffle(charRoute, rng);
            for (int j = 0; j < NUM_POINTS; j++) {
                allParticles[i][j] = charRoute.get(j);
            }
        }


        // Flatten the 2D Particles into a 1D array
        char[] flatParticles = new char[POP_SIZE * NUM_POINTS];
        for (int i = 0; i < POP_SIZE; i++) {
            System.arraycopy(allParticles[i], 0, flatParticles, i * NUM_POINTS, NUM_POINTS);
        }

        // Scatter flattened particles to all ranks
        char[] flatLocalParticles = new char[localPopSize * NUM_POINTS];
        MPI.COMM_WORLD.Scatter(flatParticles, 0, localPopSize * NUM_POINTS, MPI.CHAR,
                            flatLocalParticles, 0, localPopSize * NUM_POINTS, MPI.CHAR, 0);

        // Reshape the received data back into a 2D array
        for (int i = 0; i < localPopSize; i++) {
            System.arraycopy(flatLocalParticles, i * NUM_POINTS, localParticles[i], 0, NUM_POINTS);
        }
     } else {
        // Worker ranks receive their portion of particles
        char[] flatLocalParticles = new char[localPopSize * NUM_POINTS];
        MPI.COMM_WORLD.Scatter(null, 0, localPopSize * NUM_POINTS, MPI.CHAR,
                            flatLocalParticles, 0, localPopSize * NUM_POINTS, MPI.CHAR, 0); 

        // Reshape the received data back into a 2D array
        for (int i = 0; i < localPopSize; i++) {
            System.arraycopy(flatLocalParticles, i * NUM_POINTS, localParticles[i], 0, NUM_POINTS);
        }
     } 

        // Initialize particles for each process
        for (int i = 0; i < localPopSize; i++) {

            char[] route = localParticles[i];
            double fitness = calculateTotalDistance(route, distMatrix);
            localFitness[i] = fitness;

            //set personal best for particle
            localPBest.add(route.clone());
            localPBestFitness[i] = fitness;


        // Update local global best fitness and route if this route is better
            if (fitness < globalGBestFitness[0]) {
                globalGBestFitness[0] = fitness;
                System.arraycopy(route, 0, globalGBestRoute, 0, NUM_POINTS);
            }
        }

        // Synchronize global best fitness across all ranks
        double[] reducedGlobalGBestFitness = new double[1];
        MPI.COMM_WORLD.Allreduce(globalGBestFitness, 0, reducedGlobalGBestFitness, 0, 1, MPI.DOUBLE, MPI.MIN);

        // Broadcast the global best route from the rank that owns the global best fitness
        if (Math.abs(globalGBestFitness[0] - reducedGlobalGBestFitness[0]) < 1e-6) {
            MPI.COMM_WORLD.Bcast(globalGBestRoute, 0, NUM_POINTS, MPI.CHAR, rank);
        } else {
            MPI.COMM_WORLD.Bcast(globalGBestRoute, 0, NUM_POINTS, MPI.CHAR, 0);
        }

        // Main optimization loop
        for (int iter = 0; iter < MAX_ITER; iter++) {
            for (int i = 0; i < localPopSize; i++) {
                updateParticle(localParticles[i], localPBest.get(i), C1);
                updateParticle(localParticles[i], globalGBestRoute, C2);
                swapElements(localParticles[i]);

                double fitness = calculateTotalDistance(localParticles[i], distMatrix);
                localFitness[i] = fitness;

                if (fitness < localPBestFitness[i]) {
                    localPBestFitness[i] = fitness;
                    localPBest.set(i, localParticles[i].clone());
                }
            }

            // Find local best
            int localBestIdx = IntStream.range(0, localPBestFitness.length)
                    .reduce((a, b) -> localPBestFitness[a] < localPBestFitness[b] ? a : b)
                    .orElse(0);

            double localBestFitness = localPBestFitness[localBestIdx];
            char[] localBestRoute = localPBest.get(localBestIdx).clone(); //

            // Synchronize global best
            if (localBestFitness < globalGBestFitness[0]) {
                globalGBestFitness[0] = localBestFitness;
                System.arraycopy(localBestRoute, 0, globalGBestRoute, 0, NUM_POINTS);
            }

            // Synchronize global best fitness across all ranks
            double[] reducedGlobalBestFitness = new double[1];
            MPI.COMM_WORLD.Allreduce(globalGBestFitness, 0, reducedGlobalBestFitness, 0, 1, MPI.DOUBLE, MPI.MIN);

            // Update globalGBestFitness[0] with the globally reduced value
            globalGBestFitness[0] = reducedGlobalBestFitness[0];

            // If this rank owns the global best fitness, broadcast the corresponding route
            if (Math.abs(globalGBestFitness[0] - reducedGlobalBestFitness[0]) < 1e-6) {
                MPI.COMM_WORLD.Bcast(globalGBestRoute, 0, NUM_POINTS, MPI.CHAR, rank);
            } else {
                MPI.COMM_WORLD.Bcast(globalGBestRoute, 0, NUM_POINTS, MPI.CHAR, 0);
            }


            if (rank == 0 && (iter + 1) % 100 == 0) {
                System.out.println("Iteration " + (iter + 1) + ": Best Distance = " + globalGBestFitness[0]);
            }
        }

        if (rank == 0) {
            System.out.println("Final Best Distance: " + globalGBestFitness[0]);
            System.out.print("Best Route: ");
            for (char city : globalGBestRoute) {
                System.out.print(city + " ");
            }
            System.out.println();
        }

        MPI.Finalize();

            long endTime = System.currentTimeMillis(); // End time
            if (rank == 0) {
                long elapsedTime = endTime - startTime;
                System.out.println("Elapsed Time: " + elapsedTime + " ms");
            }
    }


    private static List<double[]> readCityCoordinates(String filename, int numCities) {
        List<double[]> coordinates = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                coordinates.add(new double[]{x, y});
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return coordinates;
    }

    private static double[][] computeDistanceMatrix(List<double[]> cityCoordinates) {
        int n = cityCoordinates.size();
        double[][] distMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double dx = cityCoordinates.get(i)[0] - cityCoordinates.get(j)[0];
                double dy = cityCoordinates.get(i)[1] - cityCoordinates.get(j)[1];
                distMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
            }
        }
        return distMatrix;
    }

    private static double calculateTotalDistance(char[] route, double[][] distMatrix) {
        double totalDistance = 0.0;
        for (int i = 0; i < route.length; i++) {
            int from = CITIES.indexOf(route[i]);
            int to = CITIES.indexOf(route[(i + 1) % route.length]);
            totalDistance += distMatrix[from][to];
        }
        return totalDistance;
    }


    private static void swapElements(char[] route) {
        int i = rng.nextInt(route.length);
        int j = rng.nextInt(route.length);
        char temp = route[i];
        route[i] = route[j];
        route[j] = temp;
    }

    private static void updateParticle(char[] route, char[] bestRoute, double influence) {
        boolean[] visited = new boolean[route.length];
        char[] newRoute = new char[route.length];
        int index = 0;

        // Incorporate bestRoute with influence probability
        for (char city : bestRoute) {
            if (rng.nextDouble() < influence && !visited[CITIES.indexOf(city)]) {
                newRoute[index++] = city;
                visited[CITIES.indexOf(city)] = true;
            }
        }

        // Fill remaining cities from route
        for (char city : route) {
            if (!visited[CITIES.indexOf(city)]) {
                newRoute[index++] = city;
                visited[CITIES.indexOf(city)] = true;
            }
        }

        // Update the original route
        System.arraycopy(newRoute, 0, route, 0, route.length);
    }
}
