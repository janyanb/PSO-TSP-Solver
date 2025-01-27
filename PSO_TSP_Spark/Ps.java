import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Ps {
    private static final int NUM_POINTS = 36;
    private static final int POP_SIZE = 300;
    private static final int MAX_ITER = 1000;
    private static final double C1 = 0.1;
    private static final double C2 = 0.8;
    public static final Random RNG = new Random();
    // Read city coordinates
    private static List<City> readCityCoordinates(String filename) {
        List<City> cities = new ArrayList<>();
        int id=0;
        try {
           List<String> lines = Files.readAllLines(Paths.get(filename));
           for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            String name = parts[0].trim();
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            cities.add(new City(name,id++, x, y));
        }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return cities;
    }

    private static double[][] computeDistanceMatrix(List<City> cities) {
        int n = cities.size();
        double[][] distMatrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                City city1 = cities.get(i);
                City city2 = cities.get(j);
                double dx = city1.getX() - city2.getX();
                double dy = city1.getY() - city2.getY();
                distMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
            }
        }
        return distMatrix;
    }
    //calculate distance between cities
    private static double calculateTotalDistance(List<City> route, double[][] distMatrix) {
        double totalDistance = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            int from = route.get(i).getId();
            int to = route.get(i + 1).getId();
            totalDistance += distMatrix[from][to];
        }


        return totalDistance;
    }
    //initialize routes randomly
      private static List<City> initializeRoute(List<City> allCities, Set<List<City>> existingRoutes) {
        List<City> route;
        do {
            route = new ArrayList<>(allCities);
            Collections.shuffle(route);
        } while (existingRoutes.contains(route));
        return route;
    }
    // creating new routes based on the influence either cognitive or social while ensuring no duplicate cities are added
    private static List<City> updateParticle(List<City> route, List<City> bestRoute, double influence) {
        int n = route.size();
        boolean[] visited = new boolean[n];
        List<City> newRoute = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (Math.random() < influence && !visited[bestRoute.get(i).getId()]) {
                newRoute.add(bestRoute.get(i));
                visited[bestRoute.get(i).getId()] = true;
            }
        }
        for (City city : route) {
            if (!visited[city.getId()]) {
                newRoute.add(city);
                visited[city.getId()] = true;
            }
        }

        return newRoute;
        }
    //randomly swap 2 cities in the route to add diversity and prevent premature convergence
    public static List<City> randomSwapCities(List<City> route) {
        int i = RNG.nextInt(route.size());
        int j = RNG.nextInt(route.size());
        Collections.swap(route, i, j);
        return route;
    }

    public static void main(String[] args) {
        // Create Spark session
        SparkSession spark = SparkSession.builder()
            .appName("PSO_TSP")
            .getOrCreate();

        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
        List<City> cities = readCityCoordinates("cities.txt");
        // now start a timer
        long startTime = System.currentTimeMillis();
        // Compute distance matrix between all cities in the file
        double[][] distMatrix = computeDistanceMatrix(cities);
        // Initialize population
        Set<List<City>> uniqueRoutes = new HashSet<>();
        List<Particle> particles = IntStream.range(0, POP_SIZE)
            .mapToObj(i -> {
                    List<City> route = initializeRoute(cities, uniqueRoutes);
                    uniqueRoutes.add(route);
                    Particle particle = new Particle(route);
                    double fitness = calculateTotalDistance(route, distMatrix);
                    particle.setFitness(fitness);
                    // Initialize personal best for the initially generated route
                    particle.setPersonalBest(new ArrayList<>(route));
                    particle.setPersonalBestFitness(fitness);
                    return particle;
                })
            .collect(Collectors.toList());
 
        // Track global best among the initial routes
        Particle globalBestParticle = particles.stream().min(Comparator.comparingDouble(Particle::getFitness)).orElseThrow(()-> new NoSuchElementException("No fitness found!!!!!! Check initialization!!!!"));
        Particle temp= new Particle(globalBestParticle);  //tracking the initial global best as variable temp to retrieve it for comparision at the end
                for (int iteration = 0; iteration < MAX_ITER; iteration++) {
            final Particle currentGlobalBest = new Particle(globalBestParticle);  //creating a final variable to use the globalBest route in the map(). This will also be updated each iteration so that new global best is used every time
            //creating an RDD to distribute among nodes
            JavaRDD<Particle> particleRDD = sc.parallelize(particles);
            List<Particle> updatedParticles = particleRDD.map(particle -> {
                    //the below transformations create new routes by calling updateParticle() with cognitive and social influence in taht order and later uses a random swap to add diversity to generated routes
                    Particle updatedParticle1 = new Particle(
                                                            updateParticle(
                                                                           particle.getRoute(),
                                                                           particle.getPersonalBest(),
                                                                           C1
                                                                           )
                                                            );

                    Particle updatedParticle2 = new Particle(
                                                             updateParticle(
                                                                            updatedParticle1.getRoute(),
                                                                            currentGlobalBest.getRoute(),
                                                                            C2
                                                                            )
                                                             );
                    Particle updatedParticle3 = new Particle(
                                                             randomSwapCities(updatedParticle2.getRoute())
                                                             );

                    double newFitness = calculateTotalDistance(updatedParticle3.getRoute(), distMatrix);
                    updatedParticle3.setFitness(newFitness);

                    // Update personal best if new route is better
                    if (newFitness < particle.getFitness()) {
                        updatedParticle3.setPersonalBest(new ArrayList<>(updatedParticle3.getRoute()));
                        updatedParticle3.setPersonalBestFitness(newFitness);
                    }
                    else {
                        // If not, update the previous personal best as the eprsonal best for each updated route
                        updatedParticle3.setPersonalBest(new ArrayList<>(particle.getPersonalBest()));
                        updatedParticle3.setPersonalBestFitness(particle.getPersonalBestFitness());
                    }
                    return updatedParticle3;
                }).collect();

            for (Particle part : updatedParticles) {

                if (part.getFitness() < globalBestParticle.getFitness()) {
                    //update Global fitness by comparing the newly generated particle fitness with the current global best
                    globalBestParticle = new Particle(part);
                }
            }

            // Update particles list with new population
            particles = updatedParticles;

            System.out.printf("Iteration %d: Best Distance = %.2f%n",
                              iteration + 1, globalBestParticle.getFitness());
                              }

        
        //print the initail best route and the final best route
        System.out.println("\nFirst Route:");
        for (int i = 0; i < temp.getRoute().size(); i++) {
            City city = temp.getRoute().get(i);
            System.out.print(city.getName());

             if (i < temp.getRoute().size() - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println();
        System.out.printf("First Distance: %.2f%n", temp.getFitness());
        System.out.println();

        System.out.println("\nFinal Best Route:");
        for (int i = 0; i < globalBestParticle.getRoute().size(); i++) {
            City city = globalBestParticle.getRoute().get(i);
            System.out.print(city.getName());

             if (i < globalBestParticle.getRoute().size() - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println();
        System.out.printf("Final Best Distance: %.2f%n", globalBestParticle.getFitness());
        long endTime = System.currentTimeMillis();
        //print the elapsed time
        System.out.println("Elapsed time: " + (endTime - startTime));
        sc.close();
        spark.stop();
    }
}
