//package tech.tgo.efusion.track;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import tech.tgo.efusion.EfusionListener;
//import tech.tgo.efusion.EfusionProcessManager;
//import tech.tgo.efusion.util.*;
//import tech.tgo.efusion.model.MissionMode;
//import tech.tgo.efusion.model.GeoMission;
//import tech.tgo.efusion.model.Target;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Timer;
//
///**
// * @author edge2ipi (https://github.com/Ausstaker)
// */
//public class AllObservationITs implements EfusionListener {
//
//    private static final Logger log = LoggerFactory.getLogger(AllObservationITs.class);
//
//    Map<String,GeoMission> missionsMap = new HashMap<String,GeoMission>();
//
//    EfusionProcessManager efusionProcessManager = new EfusionProcessManager(this);
//
//    SimulatedTargetObserver simulatedTargetObserver = new SimulatedTargetObserver();
//
//    SimulatedTargetObservationRemover simulatedTargetObservationRemover = new SimulatedTargetObservationRemover();
//
//    Timer timer = new Timer();
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
//
//    GeoMission geoMission;
//
//    @Before
//    public void configure() {
//        simulatedTargetObserver.setEfusionProcessManager(efusionProcessManager);
//        simulatedTargetObservationRemover.setEfusionProcessManager(efusionProcessManager);
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
//        target_c.setTrue_lat(-31.78); // TOP RIGHT
//        target_c.setTrue_lon(115.890);
//        target_c.setLat_move(0.0); // STATIC
//        target_c.setLon_move(0.0);
//
//        /* Configure the intended mission */
//        geoMission = new GeoMission();
//        geoMission.setMissionMode(MissionMode.track);
//        //geoMission.setTarget(new Target("MY_TGT_ID","MY_TGT_NAME"));
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
//        // Currently required for tracking, since for some reason no obs added before filter initialisation thus init conditions setting
//        geoMission.setFilterUseSpecificInitialCondition(true);
//        geoMission.setFilterSpecificInitialLat(-32.0);
//        geoMission.setFilterSpecificInitialLon(116.9);
//
////        geoMission.setFilterAOABias(1.0);
////        geoMission.setFilterTDOABias(1.0);
////        geoMission.setFilterRangeBias(1.0);
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
//        asset_a.setProvide_range(true);
//        asset_a.setProvide_tdoa(true);
//        asset_a.setProvide_aoa(true);
//        asset_a.setCurrent_loc(asset_a_coords);
//
//        asset_b.setId("B");
//        asset_b.setProvide_range(true);
//        asset_b.setProvide_tdoa(true);
//        asset_b.setProvide_aoa(true);
//        asset_b.setCurrent_loc(asset_b_coords);
//
//        asset_c.setId("C");
//        asset_c.setProvide_range(true);
//        asset_c.setProvide_tdoa(true);
//        asset_c.setProvide_aoa(true);
//        asset_c.setCurrent_loc(asset_c_coords);
//
//        asset_d.setId("D");
//        asset_d.setProvide_range(true);
//        asset_d.setProvide_tdoa(true);
//        asset_d.setProvide_aoa(true);
//        asset_d.setCurrent_loc(asset_d_coords);
//
////        asset_a.setTdoa_asset_ids(Arrays.asList(new String[]{"B","C","D"}));
////        asset_b.setTdoa_asset_ids(Arrays.asList(new String[]{"C","D"}));
////        asset_c.setTdoa_asset_ids(Arrays.asList(new String[]{"D"}));
//    }
//
//    /* Result callback */
//    @Override
//    public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
//        log.debug("Result -> GeoId: " + geoId + ", Target Id: "+target_id+",Lat: " + lat + ", Lon: " + lon + ", CEP major: " + cep_elp_maj + ", CEP minor: " + cep_elp_min + ", CEP rotation: " + cep_elp_rot);
//    }
//
//
//    @Test
//    public void testMoverNorthEast() throws Exception {
//
//        target_a.setTrue_lat(-31.98); // BOTTOM
//        target_a.setTrue_lon(116.000);
//        target_a.setLat_move(0.005); // NE
//        target_a.setLon_move(0.005);
//
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>() /// NOTE: in the nav use case the meaning of this is TestTargets
//        {{
//            put(target_a.getId(), target_a);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setAoa_rand_factor(0.0);
//        simulatedTargetObserver.setRange_rand_factor(0);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0);
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
//            put(asset_d.getId(), asset_d);
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
//
//        try {
//            efusionProcessManager.start();
//
//            Thread.sleep(60000);
//
//            timer.cancel();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testMoverNorthEast_TwoTargets() throws Exception {
//
//        target_a.setLat_move(0.005); // NE
//        target_a.setLon_move(0.005);
//        target_b.setLat_move(0.001); // NE
//        target_b.setLon_move(0.001);
//
//        Map<String,Target> targets = new HashMap<String,Target>();
//        targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
//        targets.put(target_b.getId(),new Target(target_b.getId(),target_b.getName(),new Double[]{target_b.getTrue_lat(),target_b.getTrue_lon()}));
//        efusionProcessManager.reconfigureTargets(targets);
//
//        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
//        {{
//            put(target_a.getId(), target_a);
//            put(target_b.getId(), target_b);
//            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
//            target_b.setTdoa_target_ids(Arrays.asList(new String[]{}));
//        }};
//        simulatedTargetObserver.setTestTargets(testTargets);
//
//        simulatedTargetObserver.setAoa_rand_factor(0.0);
//        simulatedTargetObserver.setRange_rand_factor(0);
//        simulatedTargetObserver.setTdoa_rand_factor(0.0);
//
//        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
//        {{
//            put(asset_a.getId(), asset_a);
//            put(asset_b.getId(), asset_b);
//            put(asset_c.getId(), asset_c);
//            put(asset_d.getId(), asset_d);
//        }};
//        simulatedTargetObserver.setTestAssets(assets);
//        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
//
//        try {
//            efusionProcessManager.start();
//
//            Thread.sleep(60000);
//
//            timer.cancel();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
////
////    @Test
////    public void testMoverNorthEast() {
////        simulatedTargetObserver.setTrue_lat(-31.98); // BOTTOM
////        simulatedTargetObserver.setTrue_lon(116.000);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(200);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(+0.005); // MOVE NE
////        simulatedTargetObserver.setLon_move(+0.005);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            put(asset_c.getId(), asset_c);
////            put(asset_d.getId(), asset_d);
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(40000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    @Test
////    public void testMoverNorthEast_B() {
////        simulatedTargetObserver.setTrue_lat(-31.895); // RIGHT
////        simulatedTargetObserver.setTrue_lon(116.124);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(200);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(+0.005); // MOVE NE
////        simulatedTargetObserver.setLon_move(+0.005);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            put(asset_c.getId(), asset_c);
////            put(asset_d.getId(), asset_d);
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(40000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    /* FAILS under normal conditions
////    *   incorrect result between obs from A ~360- to B ~0+: AOA 360-0 conundrum
////    *   Confirmed fixed with forced innovation direction fix. Not addressed by the prevailing innov pressure fix, needs further R&D */
////    @Test
////    public void testMoverNorthEast_TwoAssets() {
////        simulatedTargetObserver.setTrue_lat(-31.98); // BOTTOM
////        simulatedTargetObserver.setTrue_lon(116.000);
////        simulatedTargetObserver.setAoa_rand_factor(0.0);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setRange_rand_factor(200);
////        simulatedTargetObserver.setLat_move(+0.005); // MOVE NE
////        simulatedTargetObserver.setLon_move(+0.005);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            asset_a.setTdoa_asset_ids(Arrays.asList(new String[]{"B"}));
////            asset_b.setTdoa_asset_ids(Arrays.asList(new String[]{}));
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(40000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    /* Similar test as above, starting from target location close to 360-0 border */
////    @Test
////    public void testMoverNorthEast_B_TwoAssets() {
////        simulatedTargetObserver.setTrue_lat(-31.895); // RIGHT -31.900 is below the 360-0 crossover
////        simulatedTargetObserver.setTrue_lon(116.124);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(200);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(+0.005); // MOVE NE
////        simulatedTargetObserver.setLon_move(+0.005);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            asset_a.setTdoa_asset_ids(Arrays.asList(new String[]{"B"}));
////            asset_b.setTdoa_asset_ids(Arrays.asList(new String[]{}));
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(40000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    /* Sensitive to initial conditions, sometimes will find wrong local minimum */
////    @Test
////    public void testStationaryTarget() {
////        simulatedTargetObserver.setTrue_lat(-31.98); // LEFT
////        simulatedTargetObserver.setTrue_lon(115.80);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(50);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(0.000); // NO MOVEMENT
////        simulatedTargetObserver.setLon_move(0.000);
////
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            put(asset_c.getId(), asset_c);
////            put(asset_d.getId(), asset_d);
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(40000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    @Test
////    public void testMoverSouthWest() {
////        simulatedTargetObserver.setTrue_lat(-31.7); // TOPRIGHT
////        simulatedTargetObserver.setTrue_lon(116.08);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(50);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(-0.005); // MOVE SW
////        simulatedTargetObserver.setLon_move(-0.005);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            put(asset_c.getId(), asset_c);
////            put(asset_d.getId(), asset_d);
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(40000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    @Test
////    public void testMoverNorth() {
////        simulatedTargetObserver.setTrue_lat(-31.99); // BOTTOM
////        simulatedTargetObserver.setTrue_lon(115.95);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(50);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(+0.005); // MOVE N
////        simulatedTargetObserver.setLon_move(+0.000);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            put(asset_c.getId(), asset_c);
////            put(asset_d.getId(), asset_d);
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(30000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
////
////    @Test
////    public void testMoverNorth_RemoveRandomObservations() {
////        simulatedTargetObserver.setTrue_lat(-31.99); // BOTTOM
////        simulatedTargetObserver.setTrue_lon(115.95);
////        simulatedTargetObserver.setAoa_rand_factor(0.1);
////        simulatedTargetObserver.setRange_rand_factor(50);
////        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
////        simulatedTargetObserver.setLat_move(+0.005); // MOVE N
////        simulatedTargetObserver.setLon_move(+0.000);
////        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
////        {{
////            put(asset_a.getId(), asset_a);
////            put(asset_b.getId(), asset_b);
////            put(asset_c.getId(), asset_c);
////            put(asset_d.getId(), asset_d);
////        }};
////        simulatedTargetObserver.setTestAssets(assets);
////        timer.scheduleAtFixedRate(simulatedTargetObserver,0,999);
////        timer.scheduleAtFixedRate(simulatedTargetObservationRemover,1500, 400);
////
////        try {
////            efusionProcessManager.start();
////
////            Thread.sleep(30000);
////
////            timer.cancel();
////        }
////        catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
//}
