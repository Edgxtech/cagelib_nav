//package tech.tgo.efusion.fix;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import tech.tgo.efusion.EfusionListener;
//import tech.tgo.efusion.EfusionProcessManager;
//import tech.tgo.efusion.model.MissionMode;
//import tech.tgo.efusion.model.GeoMission;
//import tech.tgo.efusion.model.Target;
//import tech.tgo.efusion.util.ConfigurationException;
//import tech.tgo.efusion.util.SimulatedTargetObserver;
//import tech.tgo.efusion.util.TestAsset;
//import tech.tgo.efusion.util.TestTarget;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author edge2ipi (https://github.com/Ausstaker)
// */
//public class TDOAObservationFixITs implements EfusionListener {
//
//    private static final Logger log = LoggerFactory.getLogger(TDOAObservationFixITs.class);
//
//    Map<String,GeoMission> missionsMap = new HashMap<String,GeoMission>();
//
//    EfusionProcessManager efusionProcessManager = new EfusionProcessManager(this);
//
//    SimulatedTargetObserver simulatedTargetObserver = new SimulatedTargetObserver();
//
//    /* Some common asset coords to reuse */
//    double[] asset_a_coords = new double[]{-31.9, 115.98};
//    double[] asset_b_coords = new double[]{-31.88, 115.97};
//    double[] asset_c_coords = new double[]{-31.78, 115.90};
//    double[] asset_d_coords = new double[]{-32.0, 115.85};
//
//    TestAsset asset_a = new TestAsset();
//    TestAsset asset_b = new TestAsset();
//    TestAsset asset_c = new TestAsset();
//    TestAsset asset_d = new TestAsset();
//
//    TestTarget target_a = new TestTarget();
//    TestTarget target_b = new TestTarget();
//    TestTarget target_c = new TestTarget();
//    TestTarget target_d = new TestTarget();
//
//    GeoMission geoMission;
//
//    @Before
//    public void configure() {
//        simulatedTargetObserver.setEfusionProcessManager(efusionProcessManager);
//
//        target_a.setId("A");
//        target_a.setName("Target A");
//        target_a.setTrue_lat(-31.98); // BOTTOM
//        target_a.setTrue_lon(116.000);
//        target_a.setLat_move(0.0); // STATIC
//        target_a.setLon_move(0.0);
//
//        target_b.setId("B");
//        target_b.setName("Target B");
//        target_b.setTrue_lat(-31.88); // TOP RIGHT
//        target_b.setTrue_lon(115.990);
//        target_b.setLat_move(0.0); // STATIC
//        target_b.setLon_move(0.0);
//
//        target_c.setId("C");
//        target_c.setName("Target C");
//        target_c.setTrue_lat(-31.78); // TOP MIDDLE?
//        target_c.setTrue_lon(115.890);
//        target_c.setLat_move(0.0); // STATIC
//        target_c.setLon_move(0.0);
//
//        target_d.setId("D");
//        target_d.setName("Target D");
//        target_d.setTrue_lat(-32.1); // BOTTOM LEFT
//        target_d.setTrue_lon(115.790);
//        target_d.setLat_move(0.0); // STATIC
//        target_d.setLon_move(0.0);
//
//        /* Configure the intended mission */
//        geoMission = new GeoMission();
//        geoMission.setMissionMode(MissionMode.fix);
//        geoMission.setGeoId("MY_GEO_ID");
//        geoMission.setShowMeas(true);
//        geoMission.setShowCEPs(true);
//        geoMission.setShowGEOs(true);
//        geoMission.setOutputKml(true);
//        geoMission.setOutputKmlFilename("geoOutput.kml");
//        geoMission.setShowTrueLoc(true);
//        geoMission.setOutputFilterState(true);
//        geoMission.setOutputFilterStateKmlFilename("filterState.kml");
//
//        try {
//            efusionProcessManager.configure(geoMission);
//        }
//        catch (ConfigurationException ce) {
//            log.error("Error trying to configure mission, returning. Error: "+ce.getMessage());
//            ce.printStackTrace();
//            return;
//        }
//        catch (IOException ioe) {
//            log.error("IO Error trying to configure mission, returning. Error: "+ioe.getMessage());
//            ioe.printStackTrace();
//            return;
//        }
//        catch (Exception e) {
//            log.error("Error trying to configure mission, returning");
//            e.printStackTrace();
//            return;
//        }
//        log.debug("Configured Geo Mission, continuing");
//
//        /* Client side needs to manage geomission references for callback response */
//        missionsMap.put(geoMission.getGeoId(), geoMission);
//
//        /* Create some reusable test assets */
//        asset_a.setId("A");
//        asset_a.setProvide_tdoa(true);
//        asset_a.setCurrent_loc(asset_a_coords);
//
//        asset_b.setId("B");
//        asset_b.setProvide_tdoa(true);
//        asset_b.setCurrent_loc(asset_b_coords);
//
//        asset_c.setId("C");
//        asset_c.setProvide_tdoa(true);
//        asset_c.setCurrent_loc(asset_c_coords);
//
//        asset_d.setId("D");
//        asset_d.setProvide_tdoa(true);
//        asset_d.setCurrent_loc(asset_d_coords);
//
//    }
//
//    /* Result callback */
//    @Override
//    public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
//        log.debug("Result -> GeoId: "+geoId+", TargetId: "+target_id+", Lat: "+lat+", Lon: "+lon+", CEP major: "+cep_elp_maj+", CEP minor: "+cep_elp_min+", CEP rotation: "+cep_elp_rot);
//    }
//
//    /* Notice how this can innovate towards the wrong branch - depending on init conditions */
//    @Test
//    public void testBottom() throws Exception {
//        target_a.setTrue_lat(-31.98); // BOTTOM
//        target_a.setTrue_lon(116.000);
//        target_a.setLat_move(+0.000); // STATIC
//        target_a.setLon_move(+0.000);
//
//                /* Targets to be tracked by filter, specified by client */
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        /* Targets for the sim observer to report data on */
//        // TestTargets could be based on simply the existing targets intended to be tracked.
//        /// REMOVED IN NAV, moved into each testTarget, latMove, lonMove, True_lat/lon removed in NAV impl
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
//        {{
//            put(target_a.getId(), target_a);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
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
//    public void testBottom_TwoAssets() throws Exception {
//        target_a.setTrue_lat(-31.98); // BOTTOM
//        target_a.setTrue_lon(116.000);
//        target_a.setLat_move(+0.000); // STATIC
//        target_a.setLon_move(+0.000);
//
//        /* Targets to be tracked by filter, specified by client */
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        /* Targets for the sim observer to report data on */
//        // TestTargets could be based on simply the existing targets intended to be tracked.
//        /// REMOVED IN NAV, moved into each testTarget, latMove, lonMove, True_lat/lon removed in NAV impl
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
//        {{
//            put(target_a.getId(), target_a);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
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
//    public void testThreeAssets() throws Exception {
//        target_a.setTrue_lat(-31.7); // TOPRIGHT
//        target_a.setTrue_lon(116.08);
//        target_a.setLat_move(+0.000); // STATIC
//        target_a.setLon_move(+0.000);
//
//        /* Targets to be tracked by filter, specified by client */
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        /* Targets for the sim observer to report data on */
//        // TestTargets could be based on simply the existing targets intended to be tracked.
//        /// REMOVED IN NAV, moved into each testTarget, latMove, lonMove, True_lat/lon removed in NAV impl
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
//        {{
//            put(target_a.getId(), target_a);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//
//        geoMission.setFilterMeasurementError(1.0);
//
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
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
//    public void testTopRight() throws Exception {
//        target_a.setTrue_lat(-31.7); // TOPRIGHT
//        target_a.setTrue_lon(116.08);
//        target_a.setLat_move(0.000); // STATIC
//        target_a.setLon_move(0.000);
//
//        /* Targets to be tracked by filter, specified by client */
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        /* Targets for the sim observer to report data on */
//        // TestTargets could be based on simply the existing targets intended to be tracked.
//        /// REMOVED IN NAV, moved into each testTarget, latMove, lonMove, True_lat/lon removed in NAV impl
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
//        {{
//            put(target_a.getId(), target_a);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//
//        geoMission.setFilterMeasurementError(0.3);
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
//    /* Demonstrates going to separate local minimum depending on initial conditions */
//    @Test
//    public void testMiddle() throws Exception {
//        target_a.setTrue_lat(-31.9); // MIDDLE
//        target_a.setTrue_lon(115.95);
//        target_a.setLat_move(0.000); // NO MOVEMENT
//        target_a.setLon_move(0.000);
//
//        /* Targets to be tracked by filter, specified by client */
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        /* Targets for the sim observer to report data on */
//        // TestTargets could be based on simply the existing targets intended to be tracked.
//        /// REMOVED IN NAV, moved into each testTarget, latMove, lonMove, True_lat/lon removed in NAV impl
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
//        {{
//            put(target_a.getId(), target_a);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
//
//        geoMission.setFilterMeasurementError(0.3);
//        //geoMission.setFilterProcessNoise(new double[][]{{10, 0, 0, 0}, {0, 10 ,0, 0}, {0, 0, 0.0001, 0}, {0, 0, 0 ,0.0001}});
//
//        geoMission.setFilterDispatchResidualThreshold(10.0);
//        geoMission.setDispatchResultsPeriod(new Long(100));
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
//}
