//package tech.tgo.fuzer.application;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import tech.tgo.fuzer.EfusionListener;
//import tech.tgo.fuzer.EfusionProcessManager;
//import tech.tgo.fuzer.model.*;
//import tech.tgo.fuzer.util.ConfigurationException;
//import uk.me.jstott.jcoord.LatLng;
//import uk.me.jstott.jcoord.UTMRef;
//
//import java.io.IOException;
//import java.util.*;
//
//public class Runner implements EfusionListener {
//
//    private static final Logger log = LoggerFactory.getLogger(Runner.class);
//
//    Map<String,GeoMission> fuzerMissions = new HashMap<String,GeoMission>();
//
//    public static void main(String[] args) {
//        Runner runner = new Runner();
//        runner.runFuzerTest();
//    }
//
//    public void runFuzerTest() {
//        EfusionProcessManager fuzerProcess = new EfusionProcessManager(this);
//
//        /* Configure the intended mission */
//        GeoMission geoMission = new GeoMission();
//        geoMission.setMissionMode(MissionMode.track);
//        geoMission.setTarget(new Target("<TGT_ID>","<TGT-NAME>"));
//        geoMission.setGeoId("<GEOID>");
//        geoMission.setShowMeas(true);
//        geoMission.setShowCEPs(true);
//        geoMission.setShowGEOs(true);
//        geoMission.setOutputKml(true);
//        geoMission.setOutputKmlFilename("geoOutput.kml");
//        geoMission.setShowTrueLoc(true);
//
//        /* These configs are available for optional override */
//        geoMission.setDispatchResultsPeriod(new Long(1000)); // Default: 1000
//        geoMission.setFilterThrottle(null); // Default is null
//        geoMission.setFilterConvergenceResidualThreshold(0.01); // Default: 0.01
//
//        try {
//            fuzerProcess.configure(geoMission);
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
//            log.error("Error trying to configuremission, returning");
//            e.printStackTrace();
//            return;
//        }
//        log.debug("Configured Geo Mission, continuing");
//
//        /* Client side needs to manage geomission references for callback response */
//        fuzerMissions.put(geoMission.getGeoId(), geoMission);
//
//        /* For Tracker - start process and continually add new observations (one per asset), monitor result in result() callback */
//        /* For Fixer - add observations (one per asset) then start, monitor output in result() callback */
//
//        Timer timer = new Timer();
//
//        /* Test test basic filter operation - with specified observations */
////        ObservationAdderMain observationAdderMain = new ObservationAdderMain();
////        observationAdderMain.setFuzerProcess(fuzerProcess);
////        observationAdderMain.run();
//
//
//          /* To test target stationary */
////        StationaryTargetObserver stationaryTargetObserver = new StationaryTargetObserver();
////        stationaryTargetObserver.setFuzerProcess(fuzerProcess);
////        stationaryTargetObserver.setTrue_lat(-31.893999999999995);
////        stationaryTargetObserver.setTrue_lon(115.998);
////        stationaryTargetObserver.run();
//
//
//        /* To test target moving */
//        MovingTargetObserver movingTargetObserver = new MovingTargetObserver();
//        movingTargetObserver.setFuzerProcess(fuzerProcess);
////        movingTargetObserver.setTrue_lat(-31.7); // TOPRIGHT
////        movingTargetObserver.setTrue_lon(116.08);
////        movingTargetObserver.setTrue_lat(-31.99); // BOTTOM
////        movingTargetObserver.setTrue_lon(115.95);
//        movingTargetObserver.setTrue_lat(-31.929670455929934);  /// LEFT LEFT
//        movingTargetObserver.setTrue_lon(115.79549188891419);
//        timer.scheduleAtFixedRate(movingTargetObserver,0,999);
//
//        try {
//            fuzerProcess.start();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /* Client side receive raw result */
//    @Override
//    public void result(String geoId, double Xk1, double Xk2, double Xk3, double Xk4) {
//        log.debug("RAW RESULT:::: GeoId: "+geoId+", Xk1: "+Xk1+", Xk2: "+Xk2+", Xk3: "+Xk3+", Xk4: "+Xk4);
//
//        GeoMission geoMission = fuzerMissions.get(geoId);
//
//        // Need to use lat/lonZone as set up when geo mission was first set up
//        UTMRef utm = new UTMRef(Xk1,Xk2, geoMission.getLatZone(), geoMission.getLonZone());
//        LatLng ltln = utm.toLatLng();
//        log.debug("Result: Lat: "+ltln.getLat()+", Lon: "+ltln.getLng());
//    }
//}