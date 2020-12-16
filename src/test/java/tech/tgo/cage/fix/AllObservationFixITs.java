package tech.tgo.cage.fix;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tgo.cage.EfusionListener;
import tech.tgo.cage.EfusionProcessManager;
import tech.tgo.cage.compute.ComputeResults;
import tech.tgo.cage.model.MissionMode;
import tech.tgo.cage.model.GeoMission;
import tech.tgo.cage.util.ConfigurationException;
import tech.tgo.cage.util.SimulatedTargetObserver;
import tech.tgo.cage.util.TestAsset;
import tech.tgo.cage.util.TestTarget;

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
    TestTarget target_d = new TestTarget();

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
        target_c.setTrue_lat(-31.78); // TOP MIDDLE?
        target_c.setTrue_lon(115.890);
        target_c.setLat_move(0.0); // STATIC
        target_c.setLon_move(0.0);

        target_d.setId("D");
        target_d.setName("Target D");
        target_d.setTrue_lat(-32.1); // BOTTOM LEFT
        target_d.setTrue_lon(115.790);
        target_d.setLat_move(0.0); // STATIC
        target_d.setLon_move(0.0);

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
        asset_a.setProvide_tdoa(false); // tdoa not implemented in NAV
        asset_a.setProvide_aoa(true);
        asset_a.setCurrent_loc(asset_a_coords);

        asset_b.setId("B");
        asset_b.setProvide_range(true);
        asset_b.setProvide_tdoa(false);
        asset_b.setProvide_aoa(true);
        asset_b.setCurrent_loc(asset_b_coords);

        asset_c.setId("C");
        asset_c.setProvide_range(true);
        asset_c.setProvide_tdoa(false);
        asset_c.setProvide_aoa(true);
        asset_c.setCurrent_loc(asset_c_coords);

        asset_d.setId("D");
        asset_d.setProvide_range(true);
        asset_d.setProvide_tdoa(false);
        asset_d.setProvide_aoa(true);
        asset_d.setCurrent_loc(asset_d_coords);
    }

    /* Result callback - I think these will be useful for tracking only */
//    @Override
//    public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
//        log.debug("Result -> GeoId: "+geoId+", TargetId: "+target_id+", Lat: "+lat+", Lon: "+lon+", CEP major: "+cep_elp_maj+", CEP minor: "+cep_elp_min+", CEP rotation: "+cep_elp_rot);
//    }

    @Override
    public void result(ComputeResults computeResults) {
        log.debug("Result Received at Process Manager: "+"Result -> GeoId: "+computeResults.getGeoId()+", Lat: "+computeResults.getGeolocationResult().getLat()+", Lon: "+computeResults.getGeolocationResult().getLon()+", CEP major: "+computeResults.getGeolocationResult().getElp_long()+", CEP minor: "+computeResults.getGeolocationResult().getElp_short()+", CEP rotation: "+computeResults.getGeolocationResult().getElp_rot());
        log.warn("WARNING, not adding to results buffer in Process Manager REVISIT LATER");
        //this.resultBuffer.put(target_id, new GeoResult(geoId,target_id,lat,lon,cep_elp_maj,cep_elp_min,cep_elp_rot));
    }

    @Test
    public void testBottom_FourAssets() throws Exception {
        /* Targets to be tracked by filter, specified by client */
        efusionProcessManager.reconfigureTarget(target_a);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_a.getId(), target_a);
            //put(target_b.getId(), target_b);
            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
            //target_b.setTdoa_target_ids(Arrays.asList(new String[]{})); // TDOA NOT USED IN NAV
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
            put(asset_c.getId(), asset_c);
            put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();
        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBottom_TwoAssets() throws Exception {
        /* Targets to be tracked by filter, specified by client */
        //Map<String,Target> targets = new HashMap<String,Target>();
        //targets.put(target_a.getId(),new Target(target_a.getId(),target_a.getName(),new Double[]{target_a.getTrue_lat(),target_a.getTrue_lon()}));
        //targets.put(target_b.getId(),new Target(target_b.getId(),target_b.getName(),new Double[]{target_b.getTrue_lat(),target_b.getTrue_lon()}));
        //efusionProcessManager.reconfigureTargets(targets);
        //  REMOVED MULTIPLE TARGETS, SINCE NOT INTENDING TO ADDRESS THE DUEL TARGET TDOA PROBLEM
        efusionProcessManager.reconfigureTarget(target_a);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_a.getId(), target_a);
            //put(target_b.getId(), target_b);
            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
            //target_b.setTdoa_target_ids(Arrays.asList(new String[]{}));
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();

        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_FourAssets_TargetB() throws Exception {
        /* Targets to be tracked by filter, specified by client */
        efusionProcessManager.reconfigureTarget(target_b);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_b.getId(), target_b);
            //target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
            put(asset_c.getId(), asset_c);
            put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();
        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLeft_FourAssets() throws Exception {
        target_a.setTrue_lat(-31.98);
        target_a.setTrue_lon(115.80);

        /* Targets to be tracked by filter, specified by client */
        efusionProcessManager.reconfigureTarget(target_a);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_a.getId(), target_a);
            target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
            put(asset_c.getId(), asset_c);
            put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();
        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_FourAssets_TargetC() throws Exception {
        /* Targets to be tracked by filter, specified by client */
        efusionProcessManager.reconfigureTarget(target_c);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_c.getId(), target_c);
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
            put(asset_c.getId(), asset_c);
            put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();
        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_FourAssets_TargetD() throws Exception {
        /* Targets to be tracked by filter, specified by client */
        efusionProcessManager.reconfigureTarget(target_d);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_d.getId(), target_d);
            //target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
            put(asset_c.getId(), asset_c);
            put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();
        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_TwoAssets_TargetD() throws Exception {
        /* Targets to be tracked by filter, specified by client */
        efusionProcessManager.reconfigureTarget(target_d);

        /* Targets for the sim observer to report data on */
        Map<String, TestTarget> testTargets = new HashMap<String, TestTarget>()
        {{
            put(target_d.getId(), target_d);
            //target_a.setTdoa_target_ids(Arrays.asList(new String[]{}));
        }};
        simulatedTargetObserver.setTestTargets(testTargets);

        simulatedTargetObserver.setAoa_rand_factor(0.1);
        simulatedTargetObserver.setTdoa_rand_factor(0.0000001);
        simulatedTargetObserver.setRange_rand_factor(200);

        /* Assets to measure observations to/from */
        Map<String, TestAsset> assets = new HashMap<String, TestAsset>()
        {{
            //put(asset_a.getId(), asset_a);
            put(asset_b.getId(), asset_b);
            //put(asset_c.getId(), asset_c);
            put(asset_d.getId(), asset_d);
        }};
        simulatedTargetObserver.setTestAssets(assets);

        /* Execute a single observation set generation */
        simulatedTargetObserver.run();
        log.debug("Number of observations added for this test: "+efusionProcessManager.getGeoMission().getObservations().size());

        try {
            ComputeResults computeResults = efusionProcessManager.start();
            log.info("Results: "+computeResults.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
