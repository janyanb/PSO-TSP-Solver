import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Particle implements Serializable {
    private List<City> route;
    private double fitness;
    private List<City> personalBest;
    private double personalBestFitness;
    public Particle(List<City> route) {
        this.route = new ArrayList<>(route);
        this.fitness = Double.MAX_VALUE;
    }
    public void setPersonalBest(List<City> personalBest) {
        this.personalBest = personalBest;
    }

    public List<City> getPersonalBest() {
        return personalBest;
    }

    public void setPersonalBestFitness(double fitness) {
        this.personalBestFitness = fitness;
    }

    public double getPersonalBestFitness() {
        return personalBestFitness;
    }
    public List<City> getRoute() { return new ArrayList<>(route); }
    public void setRoute(List<City> route) { this.route = new ArrayList<>(route); }
    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }

    public Particle(Particle other) {
        this.route = new ArrayList<>(other.route);
        this.fitness = other.fitness;
    }
}










