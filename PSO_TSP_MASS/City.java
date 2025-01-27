package edu.uwb.css534;

import edu.uw.bothell.css.dsl.MASS.Place;

public class City extends Place {
    private String name;
    private double x;
    private double y;
    private int id;
    public static final int init = 0;

    public City(Object args) {
    }


    public Object callMethod(int method, Object[] args) {
        switch (method) {
        case init:
            return initializeCity(args);
        default:
            return null;
        }
    }

    public Object initializeCity(Object[] args) {
        this.name = (String) args[0];
        this.id = (int) args[1];
        this.x = (double) args[2];
        this.y = (double) args[3];
        System.out.println("City initialized: " + this.name + " (" + this.x + ", " + this.y + ")");
        return null; 
    }

    public String getName() { return name; }
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }

        @Override
        public String toString() {
            return name + " (" + x + "," + y + ")";
        }
}

