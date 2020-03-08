package tech.tgo.efusion.fix;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tgo.efusion.EfusionListener;
import tech.tgo.efusion.EfusionProcessManager;
import tech.tgo.efusion.model.MissionMode;
import tech.tgo.efusion.model.GeoMission;
import tech.tgo.efusion.model.Target;
import tech.tgo.efusion.util.ConfigurationException;
import tech.tgo.efusion.util.SimulatedTargetObserver;
import tech.tgo.efusion.util.TestAsset;
import tech.tgo.efusion.util.TestTarget;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Timothy Edge (timmyedge)
 */
public class AllObservationFixITs implements EfusionListener {

    private static final Logger log = LoggerFactory.getLogger(AllObservationFixITs.class);

    Map<String,GeoMission> missionsMap = new HashMap<String,GeoMission>();

    EfusionProcessManager efusionProcessManager = new EfusionProcessManager(this);

    SimulatedTargetObserver simulatedTargetObserver = new SimulatedTargetObserver();

    /* Some common asset coords to reuse */
    double[] asset_a_coords = new double[]{-31.9, 115.98};
    double[] asset_b_coords = new double[]{-31.88, 115.97};
    double[] asset_c_coords = new double[]{-31.78, 115.90};
    double[] asset_d_coords = new double[]{-32.0, 115.85};

    TestAsset asset_a = new TestAsset();
    TestAsset asset_b = new TestAsset();
    TestAsset asset_c = new TestAsset();
    TestAsset asset_d = new TestAsset();

    TestTarget target_a = new TestTarget();
    TestTarget target_b = new TestTarget();
    TestTarget target_c = new TestTarget();


    GeoMission geoMission;

    @Before
    public void configure() {
        /* Test specific configuration */
        simulatedTargetObserver.setEfusionProcessManager(efusionProcessManager);

        /* Configure the intended mission */
        geoMission = new GeoMission();
        geoMission.setMissionMode(MissionMode.fix);

        target_a.setId("A");
        target_a.setName("Target A");
        target_a.setTrue_lat(-31.98); // BOTTOM
        target_a.setTrue_lon(116.000);
        target_a.setLat_move(0.0); // STATIC
        target_a.setLon_move(0.0);

        target_b.setId("B");
        target_b.setName("Target B");
        target_b.setTrue_lat(-31.88); // TOP RIGHT
        target_b.setTrue_lon(115.990);
        target_b.setLat_move(0.0); // STATIC
        target_b.setLon_move(0.0);

        target_c.setId("C");
        target_c.setName("Target C");
        target_c.setTrue_lat(-31.78); // TOP RIGHT
        target_c.setTrue_lon(115.890);
        target_c.setLat_move(0.0); // STATIC
        target_c.setLon_move(0.0);

        //String tgt_id_a = "MY_TGT_ID";

        geoMission.setGeoId("MY_GEO_ID");
        geoMission.setShowMeas(true);
        geoMission.setShowCEPs(true);
        geoMission.setShowGEOs(true);
        geoMission.setOutputKml(true);
        geoMission.setOutputKmlFilename("geoOutput.kml");
        geoMission.setShowTrueLoc(true);
        geoMission.setOutputFilterState(true);
        geoMission.setOutputFilterStateKmlFilename("filterState.kml");

        // Optional test
//        geoMission.setFilterUseSpecificInitialCondition(true);
//        geoMission.setFilterSpecificInitialLat(-32.0);
//        geoMission.setFilterSpecificInitialLon(116.9);

        try {
            efusionProcessManager.configure(geoMission);
        }
        catch (ConfigurationException ce) {
            log.error("Error trying to configure mission, returning. Error: "+ce.getMessage());
            ce.printStackTrace();
            return;
        }
        catch (IOException ioe) {
            log.error("IO Error trying to configure mission, returning. Error: "+ioe.getMessage());
            ioe.printStackTrace();
            return;
        }
        catch (Exception e) {
            log.error("Error trying to configure mission, returning");
            e.printStackTrace();
            return;
        }
        log.debug("Configured Geo Mission, continuing");

        /* Client side needs to manage geomission references for callback response */
        missionsMap.put(geoMission.getGeoId(), geoMission);

        /* Reusable test assets */
        asset_a.setId("A");
        asset_a.setProvide_range(true);
        asset_a.setProvide_tdoa(true);
        asset_a.setProvide_aoa(true);
        asset_a.setCurrent_loc(asset_a_coords);

        asset_b.setId("B");
        asset_b.setProvide_range(true);
        asset_b.setProvide_tdoa(true);
        asset_b.setProvide_aoa(true);
        asset_b.setCurrent_loc(asset_b_coords);

        asset_c.setId("C");
        asset_c.setProvide_range(true);
        asset_c.setProvide_tdoa(true);
        asset_c.setProvide_aoa(true);
        asset_c.setCurrent_loc(asset_c_coords);

        asset_d.setId("D");
        asset_d.setProvide_range(true);
        asset_d.setProvide_tdoa(true);
        asset_d.setProvide_aoa(true);
        asset_d.setCurrent_loc(asset_d_coords);

//        asset_a.setTdoa_asset_ids(Arrays.asList(new String[]{"B","C","D"})); // NOT RELEVANT FOR NAV
//        asset_b.setTdoa_asset_ids(Arrays.asList(new String[]{"C","D"}));
//        asset_c.setTdoa_asset_ids(Arrays.asList(new String[]{"D"}));

    }

    /* Result callback */
    @Override    /// ORIG REMOVED FOR NAV
    public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
        log.debug("Result -> GeoId: "+geoId+", TargetId: "+target_id+", Lat: "+lat+", Lon: "+lon+", CEP major: "+cep_elp_maj+", CEP minor: "+cep_elp_min+", CEP rotation: "+cep_elp_rot);
    }

    @Test
    public void testBottom() {
        // TODO
        // Perhaps enforce correct targets validation upfront. && validation any new observation as needing to be linked by a previously defined target?
        //      This removes flexibility to throw new observations and dynamically adapt state matrix. (A disadvantage)
        //
        //

        /* Client currently expected to implement logic that selects which targets should be tracked. Future auto mode could extract these from observations and reinitialise filter
        / IF new targets are to be added dynamically, client can still choose to stop and start, while preserving latest states as first initial guesses */
        Map<String,Target> targets = new HashMap<String,Target>();
        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName()));
        targets.put(target_b.getId(),new Target(target_b.getId(),target_b.getName()));
        //geoMission.setTargets(targets);
        efusionProcessManager.reconfigureTargets(targets);


        // Create a set of test targets for generating observations from (for test purposes, does not have to directly match geoMission targets set to test handling observations to untracked target)
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>() /// NOTE: in the nav use case the meaning of this is TestTargets
        {{
            put(target_a.getId(), target_a);
            put(target_b.getId(), target_b);
            target_a.setTdoa_target_ids(Arrays.asList(new String[]{"B"}));
            target_b.setTdoa_target_ids(Arrays.asList(new String[]{}));
            //put(target_c.getId(), target_c);
            //target_c.setTdoa_target_ids(Arrays.asList(new String[]{}));
            //put(asset_d.getId(), asset_d);
        }};

        // TODO, testTargets can be based on simply the existing targets intended to be tracked.
        simulatedTargetObserver.setTestTargets(testTargets);



//        simulatedTargetObserver.setTrue_lat(-31.98); // BOTTOM   /// REMOVED IN NAV, moved into each testTarget
//        simulatedTargetObserver.setTrue_lon(116.000);
        simulatedTargetObserver.setAoa_rand_factor(0.0);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);
//        simulatedTargetObserver.setLat_move(0.0); // STATIC  /// REMOVED IN NAV, moved into each testTarget
//        simulatedTargetObserver.setLon_move(0.0);
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>() /// NOTE: in the nav use case the meaning of this is TestTargets
        {{
            put(asset_a.getId(), asset_a);   /// THE NUMBER OF ASSETS ARE THE NUMBER OF STATIC TX's that are measured.
            put(asset_b.getId(), asset_b);
//            asset_a.setTdoa_asset_ids(Arrays.asList(new String[]{"B"})); /// IRRELEVANT IN NAV, NOW SHARED BTWN TARGETS (i.e. my platforms)
//            asset_b.setTdoa_asset_ids(Arrays.asList(new String[]{}));
            //put(asset_c.getId(), asset_c);
            //put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);
        /* Execute a single observation set generation */
        simulatedTargetObserver.run();

        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            Thread thread = efusionProcessManager.start();
            thread.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

//        try {                      /// DOESNT SEEM TO WORK
//            Thread.sleep(1000);
//        }
//        catch (Exception e) {
//
//        }
//
//        log.info("Running second observation set");
//        simulatedTargetObserver.run();
    }

//    @Test
//    public void testBottom_TwoAssets() {
//        simulatedTargetObserver.setTrue_lat(-31.98); // BOTTOM
//        simulatedTargetObserver.setTrue_lon(116.000);
//        simulatedTargetObserver.setAoa_rand_factor(0.0);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//        simulatedTargetObserver.setRange_rand_factor(200);
//        simulatedTargetObserver.setLat_move(0.0); // STATIC
//        simulatedTargetObserver.setLon_move(0.0);
//
//        geoMission.setFilterConvergenceResidualThreshold(0.01);
//
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            asset_a.setTdoa_asset_ids(Arrays.asList(new String[]{"B"}));
//            asset_b.setTdoa_asset_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        simulatedTargetObserver.run();
//
//        try {
//            Thread thread = efusionProcessManager.start();
//            thread.join();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testLeft() {
//        simulatedTargetObserver.setTrue_lat(-31.98); // LEFT
//        simulatedTargetObserver.setTrue_lon(115.80);
//        simulatedTargetObserver.setAoa_rand_factor(0.1);
//        simulatedTargetObserver.setRange_rand_factor(50);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//        simulatedTargetObserver.setLat_move(0.0); // STATIC
//        simulatedTargetObserver.setLon_move(0.0);
//
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
//            put(asset_d.getId(), asset_d);
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        simulatedTargetObserver.run();
//
//        try {
//            Thread thread = efusionProcessManager.start();
//            thread.join();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testTopRight() {
//        simulatedTargetObserver.setTrue_lat(-31.7); // TOPRIGHT
//        simulatedTargetObserver.setTrue_lon(116.08);
//        simulatedTargetObserver.setAoa_rand_factor(0.1);
//        simulatedTargetObserver.setRange_rand_factor(50);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//        simulatedTargetObserver.setLat_move(0.0); // STATIC
//        simulatedTargetObserver.setLon_move(0.0);
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
//            put(asset_d.getId(), asset_d);
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        simulatedTargetObserver.run();
//
//        try {
//            Thread thread = efusionProcessManager.start();
//            thread.join();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testTop() {
//        simulatedTargetObserver.setTrue_lat(-31.7); // TOP
//        simulatedTargetObserver.setTrue_lon(115.80);
//        simulatedTargetObserver.setAoa_rand_factor(0.1);
//        simulatedTargetObserver.setRange_rand_factor(50);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//        simulatedTargetObserver.setLat_move(0.0); // STATIC
//        simulatedTargetObserver.setLon_move(0.0);
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
//            put(asset_d.getId(), asset_d);
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        simulatedTargetObserver.run();
//
//        try {
//            Thread thread = efusionProcessManager.start();
//            thread.join();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testRight() {
//        simulatedTargetObserver.setTrue_lat(-31.895); // RIGHT
//        simulatedTargetObserver.setTrue_lon(116.124);
//        simulatedTargetObserver.setAoa_rand_factor(0.1);
//        simulatedTargetObserver.setRange_rand_factor(50);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//        simulatedTargetObserver.setLat_move(0.0); // STATIC
//        simulatedTargetObserver.setLon_move(0.0);
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
//            put(asset_d.getId(), asset_d);
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        simulatedTargetObserver.run();
//
//        try {
//            Thread thread = efusionProcessManager.start();
//            thread.join();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
