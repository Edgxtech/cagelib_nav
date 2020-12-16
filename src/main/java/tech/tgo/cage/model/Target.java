package tech.tgo.cage.model;

import static java.util.Objects.isNull;

/**
 * @author Timothy Edge (timmyedge)
 */
public class Target {
    String id;
    String name;
    double[] current_loc;

    double elp_major;
    double elp_minor;
    double elp_rot;

    /* Used for Dev and Testing, field validation if true location is known, library plotting functions can plot as reference point */
    Double[] true_current_loc;

    public Target(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /* Used for testing */
    public Target(String id, String name, Double[] true_current_loc) {
        this.id = id;
        this.name = name;
        this.true_current_loc = true_current_loc;
    }

    public Target() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double[] getCurrent_loc() {
        return current_loc;
    }

    public void setCurrent_loc(double[] current_loc) {
        this.current_loc = current_loc;
    }

    public Double[] getTrue_current_loc() {
        return true_current_loc;
    }

    public void setTrue_current_loc(Double[] true_current_loc) {
        this.true_current_loc = true_current_loc;
    }

    public double getElp_major() {
        return elp_major;
    }

    public void setElp_major(double elp_major) {
        this.elp_major = elp_major;
    }

    public double getElp_minor() {
        return elp_minor;
    }

    public void setElp_minor(double elp_minor) {
        this.elp_minor = elp_minor;
    }

    public double getElp_rot() {
        return elp_rot;
    }

    public void setElp_rot(double elp_rot) {
        this.elp_rot = elp_rot;
    }

    @Override
    public String toString() {
        return "Id: " + ((isNull(id)) ? "" : id) +
                ", Name: " + ((isNull(name)) ? "" : name) +
                ", Lat: " + ((isNull(current_loc)) ? "" : current_loc[0]) +
                ", Lon: " + ((isNull(current_loc)) ? "" : current_loc[1]);
    }
}
