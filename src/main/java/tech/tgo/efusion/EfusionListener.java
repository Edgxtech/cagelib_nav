package tech.tgo.efusion;

/**
 * @author Timothy Edge (timmyedge)
 */
public interface EfusionListener {
    public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot); // ORIG REMOVED IN NAV
    //public void result(String geoId, Object[][] results);
}