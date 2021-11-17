package au.com.ausstaker.cage;

import au.com.ausstaker.cage.compute.ComputeResults;

/**
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public interface EfusionListener {
    // DEPRCATED - public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot); // ORIG REMOVED IN NAV
    public void result(ComputeResults computeResults);
    //public void result(String geoId, Object[][] results);
}