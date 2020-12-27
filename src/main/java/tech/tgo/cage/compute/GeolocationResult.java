package tech.tgo.cage.compute;

import tech.tgo.cage.model.GeolocationResultStatus;

import static java.util.Objects.isNull;

public class GeolocationResult {
    //Boolean processed_ok;
    GeolocationResultStatus status;
    String status_message;
    double residual;
    double residual_rk;
    double lat;
    double lon;
    double elp_long;
    double elp_short;
    double elp_rot;

    public double getResidual() {
        return residual;
    }

    public void setResidual(double residual) {
        this.residual = residual;
    }

    public double getResidual_rk() {
        return residual_rk;
    }

    public void setResidual_rk(double residual_rk) {
        this.residual_rk = residual_rk;
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

    public double getElp_long() {
        return elp_long;
    }

    public void setElp_long(double elp_long) {
        this.elp_long = elp_long;
    }

    public double getElp_short() {
        return elp_short;
    }

    public void setElp_short(double elp_short) {
        this.elp_short = elp_short;
    }

    public double getElp_rot() {
        return elp_rot;
    }

    public void setElp_rot(double elp_rot) {
        this.elp_rot = elp_rot;
    }

    public GeolocationResultStatus getStatus() {
        return status;
    }

    public void setStatus(GeolocationResultStatus status) {
        this.status = status;
    }

    public String getStatus_message() {
        return status_message;
    }

    public void setStatus_message(String status_message) {
        this.status_message = status_message;
    }

    @Override
    public String toString() {
        return "Residual: " + ((isNull(residual)) ? "" : residual) +
                ", Lat: " + ((isNull(lat)) ? "" : lat) +
                ", Lon: " + ((isNull(lon)) ? "" : lon) +
                ", Elp_Long: " + ((isNull(elp_long)) ? "" : elp_long) +
                ", Elp_Short: " + ((isNull(elp_short)) ? "" : elp_short) +
                ", Elp_Rot: " + ((isNull(elp_rot)) ? "" : elp_rot) +
                ", Status: " + ((isNull(status)) ? "" : status.name()) +
                ", Status Message: " + ((isNull(status_message)) ? "" : status_message);
    }
}
