package tech.tgo.cage.model;

import java.util.Date;

public class GeoResult {
    String geoId;
    String target_id;
    double lat;
    double lon;
    double cep_elp_maj;
    double cep_elp_min;
    double cep_elp_rot;
    Date datetime;

    public GeoResult(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
        this.geoId = geoId;
        this.target_id = target_id;
        this.lat = lat;
        this.lon = lon;
        this.cep_elp_maj = cep_elp_maj;
        this.cep_elp_min = cep_elp_min;
        this.cep_elp_rot = cep_elp_rot;
        this.datetime = new Date();
    }

    public String toString() {
        return "Result -> GeoId: "+(geoId!=null ? geoId : "null")+", TargetId: "+(target_id!=null ? target_id : "null");
        // +", Lat: "+lat+", Lon: "+lon+", CEP major: "+cep_elp_maj+", CEP minor: "+cep_elp_min+", CEP rotation: "+cep_elp_rot
    }

    public String getGeoId() {
        return geoId;
    }

    public void setGeoId(String geoId) {
        this.geoId = geoId;
    }

    public String getTarget_id() {
        return target_id;
    }

    public void setTarget_id(String target_id) {
        this.target_id = target_id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getCep_elp_maj() {
        return cep_elp_maj;
    }

    public void setCep_elp_maj(double cep_elp_maj) {
        this.cep_elp_maj = cep_elp_maj;
    }

    public double getCep_elp_min() {
        return cep_elp_min;
    }

    public void setCep_elp_min(double cep_elp_min) {
        this.cep_elp_min = cep_elp_min;
    }

    public double getCep_elp_rot() {
        return cep_elp_rot;
    }

    public void setCep_elp_rot(double cep_elp_rot) {
        this.cep_elp_rot = cep_elp_rot;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }
}
