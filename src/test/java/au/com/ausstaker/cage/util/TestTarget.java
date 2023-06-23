package au.com.ausstaker.cage.util;

import au.com.ausstaker.cage.model.Target;

import java.util.List;

/**
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public class TestTarget extends Target {
    Boolean provide_range;
    Boolean provide_aoa;
    Boolean provide_tdoa;
    List<String> tdoa_target_ids;
    double true_lat;
    double true_lon;
    double lat_move;
    double lon_move;

    public Boolean getProvide_range() {
        return provide_range;
    }

    public void setProvide_range(Boolean provide_range) {
        this.provide_range = provide_range;
    }

    public Boolean getProvide_aoa() {
        return provide_aoa;
    }

    public void setProvide_aoa(Boolean provide_aoa) {
        this.provide_aoa = provide_aoa;
    }

    public Boolean getProvide_tdoa() {
        return provide_tdoa;
    }

    public void setProvide_tdoa(Boolean provide_tdoa) {
        this.provide_tdoa = provide_tdoa;
    }

    public List<String> getTdoa_target_ids() {
        return tdoa_target_ids;
    }

    public void setTdoa_target_ids(List<String> tdoa_target_ids) {
        this.tdoa_target_ids = tdoa_target_ids;
    }

    public double getTrue_lat() {
        return true_lat;
    }

    public void setTrue_lat(double true_lat) {
        this.true_lat = true_lat;
    }

    public double getTrue_lon() {
        return true_lon;
    }

    public void setTrue_lon(double true_lon) {
        this.true_lon = true_lon;
    }

    public double getLat_move() {
        return lat_move;
    }

    public void setLat_move(double lat_move) {
        this.lat_move = lat_move;
    }

    public double getLon_move() {
        return lon_move;
    }

    public void setLon_move(double lon_move) {
        this.lon_move = lon_move;
    }
}
