package tech.tgo.cage;

import tech.tgo.cage.compute.ComputeResults;

/**
 * @author Timothy Edge (timmyedge)
 */
public interface EfusionListener {
    // DEPRCATED - public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot); // ORIG REMOVED IN NAV
    public void result(ComputeResults computeResults);
    //public void result(String geoId, Object[][] results);
}