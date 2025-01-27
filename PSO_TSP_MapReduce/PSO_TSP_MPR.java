import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class PSO_TSP_MPR {
    public static final int NUM_POINTS = 36;
    public static final int POP_SIZE = 300;
    public static final int MAX_ITER = 1000;
    public static final double W = 0.8;
    public static final double C1 = 0.1;
    public static final double C2 = 0.9;

    public static final String CITIES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static final Random RNG = new Random();

    public static List<ArrayList<Double>> readCityCoordinates(String filename, int numCities) {
        ArrayList<ArrayList<Double>> coordinates = new ArrayList<>();
        File file = new File(filename);
        try {
            Scanner sc = new Scanner(file);
            int cityIndex = 0;
            while (cityIndex < numCities && sc.hasNextLine()) {
                String[] currLine = sc.nextLine().trim().split("\\s+");
                ArrayList<Double> currCity = new ArrayList<>();
                currCity.add(Double.valueOf(currLine[1]));
                currCity.add(Double.valueOf(currLine[2]));
                coordinates.add(currCity);
                cityIndex++;
            }
            //for (int i = 0; i < numCities; i++) System.out.println("City " + CITIES.charAt(i) + ": (" + coordinates.get(i).get(0) + ", " + coordinates.get(i).get(1) + ")");
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    public static List<ArrayList<Double>> computeDistanceMatrix(List<ArrayList<Double>> cityCoordinates) {
        ArrayList<ArrayList<Double>> distMatrix = new ArrayList<>();
        int n = cityCoordinates.size();
        for (int i = 0; i < n; i++) {
            ArrayList<Double> currRow = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                double dx = cityCoordinates.get(i).get(0) - cityCoordinates.get(j).get(0);
                double dy = cityCoordinates.get(i).get(1) - cityCoordinates.get(j).get(1);
                currRow.add(Math.sqrt(dx * dx + dy * dy));
            }
            distMatrix.add(currRow);
        }
        return distMatrix;
    }

    public static double calculateTotalDistance(List<Character> route, List<ArrayList<Double>> distMatrix) {
        double totalDistance = 0.0;
        int n = route.size();
        for (int i = 0; i < n; i++) {
            int start = CITIES.indexOf(route.get(i));
            int end = CITIES.indexOf(route.get((i + 1) % n));
            totalDistance += distMatrix.get(start).get(end);
        }
        return totalDistance;
    }

    public static List<Character> initializeRoute(int numPoints, List<ArrayList<Character>> existingParticles) {
        String cityCandidates = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        List<Character> charRoute = cityCandidates.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        boolean uniqueRoute = false;
        while (!uniqueRoute) {
            Collections.shuffle(charRoute, new Random());
            uniqueRoute = true;
            for (ArrayList<Character> particle: existingParticles) {
                if (charRoute.equals(particle)) {
                    uniqueRoute = false;
                    break;
                }
            }
        }
        //System.out.print("Initialized route: ");
        //for (Character c: charRoute) System.out.print(c + " ");
        //System.out.println();
        return charRoute;
    }

    public static List<Character> swapElements(List<Character> route) {
        int i = RNG.nextInt(Integer.MAX_VALUE) % route.size();
        int j = RNG.nextInt(Integer.MAX_VALUE) % route.size();
        Collections.swap(route, i, j);
        return route;
    }

    public static List<Character> updateParticle(List<Character> route, List<Character> bestRoute, double influence) {
        //for (Character c1: route) System.out.print(c1 + " ");
        //System.out.println();
        //for (Character c2: bestRoute) System.out.print(c2 + " ");
        //System.out.println();
        int n = route.size();
        boolean[] visited = new boolean[n];
        ArrayList<Character> newRoute = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (Math.random() < influence && !visited[CITIES.indexOf(bestRoute.get(i))]) {
                newRoute.add(bestRoute.get(i));
                visited[CITIES.indexOf(bestRoute.get(i))] = true;
            }
        }
        for (int j = 0; j < n; j++) {
            if (!visited[CITIES.indexOf(route.get(j))]) {
                newRoute.add(route.get(j));
                visited[CITIES.indexOf(route.get(j))] = true;
            }
        }
        return newRoute;
    }

    public static double proTSP(String startingRoute) {
        //List<ArrayList<Double>> cityCoordinates = readCityCoordinates("cities.txt", NUM_POINTS);
        List<ArrayList<Double>> cityCoordinates = new ArrayList<>();
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(26.0, 68.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(81.0, 57.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(88.0, 15.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(69.0, 64.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(58.0, 25.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(17.0, 60.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(1.0, 69.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(58.0, 5.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(16.0, 24.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(43.0, 40.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(12.0, 60.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(32.0, 64.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(91.0, 38.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(3.0, 51.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(17.0, 28.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(94.0, 43.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(97.0, 75.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(52.0, 85.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(90.0, 21.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(1.0, 48.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(46.0, 19.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(8.0, 0.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(88.0, 19.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(5.0, 57.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(43.0, 0.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(49.0, 7.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(61.0, 33.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(23.0, 4.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(71.0, 27.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(55.0, 88.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(7.0, 49.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(83.0, 4.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(24.0, 35.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(42.0, 66.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(8.0, 43.0)));
        cityCoordinates.add(new ArrayList<Double>(Arrays.asList(15.0, 55.0)));
        List<ArrayList<Double>> distMatrix = computeDistanceMatrix(cityCoordinates);
        ArrayList<ArrayList<Character>> particles = new ArrayList<>();
        double[] fitness = new double[POP_SIZE];
        ArrayList<ArrayList<Character>> pbest = new ArrayList<>();
        double[] pbestFitness = new double[POP_SIZE];
        //ArrayList<Character> gbestRoute = new ArrayList<>();
        List<Character> gbestRoute = startingRoute.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        double gbestFitness = calculateTotalDistance(gbestRoute, distMatrix);
        for (int i = 0; i < POP_SIZE; i++) {
            fitness[i] = Double.MAX_VALUE;
            pbestFitness[i] = Double.MAX_VALUE;
            particles.add(new ArrayList<>(initializeRoute(NUM_POINTS, particles)));
            fitness[i] = calculateTotalDistance(particles.get(i), distMatrix);
            pbest.add(particles.get(i));
            pbestFitness[i] = fitness[i];
            if (fitness[i] < gbestFitness) {
                gbestFitness = fitness[i];
                gbestRoute = particles.get(i);
            }
        }
        for (int iter = 0; iter < MAX_ITER; iter++) {
            for (int j = 0; j < POP_SIZE; j++) {
                //System.out.println(pbest.get(j).size());
                List<Character> newRoute1 = updateParticle(particles.get(j), pbest.get(j), C1);
                //System.out.println(newRoute1.size());
                List<Character> newRoute2 = updateParticle(newRoute1, gbestRoute, C2);
                List<Character> newRoute3 = swapElements(newRoute2);
                particles.set(j, new ArrayList<>(newRoute3));
                fitness[j] = calculateTotalDistance(newRoute3, distMatrix);
                if (fitness[j] < pbestFitness[j]) {
                    pbestFitness[j] = fitness[j];
                    pbest.set(j, new ArrayList<>(newRoute3));
                }
                if (fitness[j] < gbestFitness) {
                    gbestFitness = fitness[j];
                    gbestRoute = new ArrayList<>(newRoute3);
                }
            }
            System.out.println("Iteration " + (iter + 1) + ": Best Distance = " + gbestFitness);
        }
        System.out.println("Final best distance: " + gbestFitness);
        System.out.print("Best route: ");
        for (Character c: gbestRoute) System.out.print(c + " ");
        System.out.println();
        return gbestFitness;
    }

    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        
        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            String line = value.toString();
            String res = String.valueOf(proTSP(line));
            output.collect(new Text("1"), new Text(res));
        }
    }
        
    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            double min = Double.MAX_VALUE;
            while (values.hasNext()) {
                String currVal = values.next().toString();
                if (min > Double.valueOf(currVal)) {
                    min = Double.valueOf(currVal);
                }
            }
            output.collect(new Text("Shortest trip"), new Text(String.valueOf(min)));
        }
    }
    
    public static void main(String[] args) throws Exception {
        JobConf conf = new JobConf(PSO_TSP_MPR.class);
        conf.setJobName("TSP");
        
        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);
        
        conf.setMapperClass(Map.class);
        conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);
        
        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);
        
        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));
        
        long startTime = System.nanoTime();
        JobClient.runJob(conf);
        long endTime = System.nanoTime();
        String totalTimeStr = String.valueOf(((double) endTime - startTime) / 1000000000);
        System.out.println("Execution time: " + totalTimeStr + " seconds");
    }
}
