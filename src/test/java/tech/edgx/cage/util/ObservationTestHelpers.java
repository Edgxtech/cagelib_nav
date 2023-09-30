package tech.edgx.cage.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservationTestHelpers {

    private static final Logger log = LoggerFactory.getLogger(ObservationTestHelpers.class);

    public static double getRangeMeasurement(double a_y, double a_x, double true_y, double true_x, double range_rand_factor) {
        return Math.sqrt(Math.pow(a_y-true_y,2) + Math.pow(a_x-true_x,2)) + (Math.random()-0.5)*range_rand_factor;
    }

    public static double getRangeMeasurementHaversine(double lat1, double lon1, double lat2, double lon2, double range_rand_factor) {
        // Range meas 2 - haversine
        double R = 6371e3;
        double l1 = lat1 * Math.PI/180;
        double l2 = lat2 * Math.PI/180;
        double delta_a = (l2 - l1) * Math.PI/180;
        double delta_b = (lon2-lon1) * Math.PI/180;
        double a = Math.sin(delta_a/2) * Math.sin(delta_a/2) + Math.cos(l1) * Math.cos(l2) * Math.sin(delta_b/2) * Math.sin(delta_b/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    public static double getTdoaMeasurement(double a_y, double a_x, double b_y, double b_x, double true_y, double true_x, double tdoa_rand_factor) {
        return (Math.sqrt(Math.pow(a_y-true_y,2) + Math.pow(a_x-true_x,2))
                - Math.sqrt(Math.pow(b_y-true_y,2) + Math.pow(b_x-true_x,2)))/Helpers.SPEED_OF_LIGHT
                + (Math.random()-0.5)*tdoa_rand_factor;
    }

    public static double getAoaMeasurement(double a_y, double a_x, double true_y, double true_x, double aoa_rand_factor) {
        double meas_aoa = Math.atan((a_y-true_y)/(a_x-true_x)) + (Math.random()-0.5)*aoa_rand_factor;
        log.debug("Meas AOA: "+meas_aoa);

        if (true_x < a_x) {
            meas_aoa = meas_aoa + Math.PI;
        }
        if (true_y<a_y && true_x>=a_x) {
            meas_aoa = (Math.PI- Math.abs(meas_aoa)) + Math.PI;
        }
        log.debug("Meas AOA (adjusted): "+meas_aoa);
        return meas_aoa;
    }
}
