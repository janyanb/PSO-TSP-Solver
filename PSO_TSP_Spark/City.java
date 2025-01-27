import java.io.Serializable;

public class City implements Serializable {
    private String name;
    private double x;
    private double y;
    private int id;
    public City(String name,int id, double x, double y) {
        this.name = name;
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getName() { return name; }
    public int getId() {
        return id;
    }
    public double getX() { return x; }
    public double getY() { return y; }
}