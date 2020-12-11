package tech.tgo.efusion;

import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tgo.efusion.compute.ComputeProcessor;
import tech.tgo.efusion.compute.ComputeResults;
import tech.tgo.efusion.model.*;
import tech.tgo.efusion.util.ConfigurationException;
import tech.tgo.efusion.util.EfusionValidator;
import tech.tgo.efusion.util.Helpers;
import tech.tgo.efusion.util.MyMaths;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Geolocation fusion and tracking, using custom extended kalman filter implementation
 *
 * @author Timothy Edge (timmyedge)
 */
public class EfusionProcessManager implements Serializable, EfusionListener {

    private static final Logger log = LoggerFactory.getLogger(EfusionProcessManager.class);

    EfusionListener actionListener;

    GeoMission geoMission;

    ComputeProcessor computeProcessor;

//    // TODO, consider holding a longer history here
    Map<String,GeoResult> resultBuffer = new HashMap<String,GeoResult>();

    public EfusionProcessManager(EfusionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void removeObservation(Long observationId) throws Exception {
        Observation obs = this.geoMission.observations.get(observationId);
        removeObservation(obs);
    }

    public void removeObservation(Observation obs) throws Exception {
        log.debug("Removing observation: "+obs.getAssetId()+","+obs.getObservationType().name());
        this.geoMission.observations.remove(obs.getId());

        /* If asset has no other linked observations, remove it */
        boolean hasOtherObs = false;
        for (Map.Entry<Long, Observation> o : this.geoMission.observations.entrySet()) {
            if (o.getValue().getAssetId().equals(obs.getAssetId())) {
                hasOtherObs=true;
                break;
            }
        }
        if (!hasOtherObs) {
            this.geoMission.getAssets().remove(obs.getAssetId());
        }

        // Remove plottable measurement
        if (obs.getObservationType().equals(ObservationType.range)) {
            this.geoMission.circlesToShow.remove(obs.getId());
        }
        else if (obs.getObservationType().equals(ObservationType.tdoa)) {
            this.geoMission.hyperbolasToShow.remove(obs.getId());
        }
        else if (obs.getObservationType().equals(ObservationType.aoa)) {
            this.geoMission.linesToShow.remove(obs.getId());
        }
    }

    public void addObservation(Observation obs) throws Exception {
        //log.debug("Validating observation against number of targets: "+this.geoMission.getTargets().size());
        //EfusionValidator.validate(obs, this.geoMission.getTargets().keySet());
        // NOT NEEDED SNET
        EfusionValidator.validate(obs);

        /* Set previous measurement here, if this is a repeated measurement */
        if (this.getGeoMission().getObservations().get(obs.getId()) != null) {
            Observation prev_obs = this.getGeoMission().getObservations().get(obs.getId());
            if (prev_obs.getMeas()!=null) {
                log.trace("Setting previous observation for: "+prev_obs.getId()+", type: "+prev_obs.getObservationType().name()+", as: "+prev_obs.getMeas());
                obs.setMeas_prev(prev_obs.getMeas());
            }
        }

        log.debug("Adding observation: "+obs.getAssetId()+","+obs.getObservationType().name()+","+obs.getMeas()+",ID:"+obs.getId()+", Target: "+obs.getTargetId());
        this.geoMission.getObservations().put(obs.getId(), obs);

        Object[] zones = Helpers.getUtmLatZoneLonZone(obs.getLat(), obs.getLon());
        obs.setY_latZone((char)zones[0]);
        obs.setX_lonZone((int)zones[1]);

        /* Rudimentary - use zones attached to the most recent observation. Perhaps make this a system wide property */
        this.geoMission.setLatZone(obs.getY_latZone());
        this.geoMission.setLonZone(obs.getX_lonZone());

        double[] utm_coords = Helpers.convertLatLngToUtmNthingEasting(obs.getLat(), obs.getLon());
        obs.setY(utm_coords[0]);
        obs.setX(utm_coords[1]);
        log.debug("Asset:"+obs.getY()+","+obs.getX());

        Asset asset = new Asset(obs.getAssetId(),new double[]{obs.getLat(),obs.getLon()});
        this.geoMission.getAssets().put(obs.getAssetId(),asset);



        if (this.geoMission.getShowMeas()) {
            /* RANGE MEASUREMENT */
            if (obs.getObservationType().equals(ObservationType.range)) {
                List<double[]> measurementCircle = new ArrayList<double[]>();
                for (double theta = (1 / 2) * Math.PI; theta <= (5 / 2) * Math.PI; theta += 0.2) {
                    UTMRef utmMeas = new UTMRef(obs.getMeas() * Math.cos(theta) + obs.getX(), obs.getMeas() * Math.sin(theta) + obs.getY(), this.geoMission.getLatZone(), this.geoMission.getLonZone());
                    LatLng ltln = utmMeas.toLatLng();
                    double[] measPoint = {ltln.getLat(), ltln.getLng()};
                    measurementCircle.add(measPoint);
                }
                this.geoMission.circlesToShow.add(obs.getId());
                obs.setCircleGeometry(measurementCircle);
            }

            /* TDOA MEASUREMENT */
            if (obs.getObservationType().equals(ObservationType.tdoa)) {
                log.debug("Defining obs hyperbola geometry");
                List<double[]> measurementHyperbola = new ArrayList<double[]>();

                // COMPROMISE, use the current target estimated locations
                //   -- However since this is the addObservation routine, may not even have a state estimate yet
                //  For the given observation target id's, check if there is a suitable state estimate available for those targets
                //double[][] utm_x_y = computeProcessor.getCurrentEstimatesForTargets
                // ALT, if there are current state estimates for the two targets, plot hyperbola from them
                GeoResult r_a = this.resultBuffer.get(obs.getTargetId());
                GeoResult r_b = this.resultBuffer.get(obs.getTargetId_b());
                /// TODO, problem here GM object from computeProcessor has the buffer, this one doesn't
                log.debug("Geo result_a null: "+(r_a==null) + ", Geo result_b null: "+(r_b==null));

                if ((r_a!=null) && (r_b!=null)) {
                    log.debug("Using target position for TDOA plot 1/2: "+r_a.toString());
                    log.debug("Using target position for TDOA plot 2/2: "+r_b.toString());

                    double[] r_a_utm = Helpers.convertLatLngToUtmNthingEasting(r_a.getLat(), r_a.getLon()); // RETURNS IN [NTHING===Y , EASTING===X]
                    double[] r_b_utm = Helpers.convertLatLngToUtmNthingEasting(r_b.getLat(), r_b.getLon());

                    double c = Math.sqrt(Math.pow((r_a_utm[1] - r_b_utm[1]), 2) + Math.pow((r_a_utm[0] - r_b_utm[0]), 2)) / 2; // focus length from origin, +-c respectively
                    double a = (obs.getMeas() * Helpers.SPEED_OF_LIGHT) / 2;
                    double b = Math.sqrt(Math.abs(Math.pow(c, 2) - Math.pow(a, 2))); // c = sqrt(a^2+b^2)
                    double ca = (r_b_utm[1] - r_a_utm[1]) / (2 * c);
                    double sa = (r_b_utm[0] - r_a_utm[0]) / (2 * c); //# COS and SIN of rot angle
                    for (double t = -2; t <= 2; t += 0.1) {
                        double X = a * Math.cosh(t);
                        double Y = b * Math.sinh(t); //# Hyperbola branch
                        double x = (r_a_utm[1] + r_b_utm[1]) / 2 + X * ca - Y * sa; //# Rotated and translated
                        double y = (r_a_utm[0] + r_b_utm[0]) / 2 + X * sa + Y * ca;
                        UTMRef utmMeas = new UTMRef(x, y, this.geoMission.getLatZone(), this.geoMission.getLonZone());
                        LatLng ltln = utmMeas.toLatLng();
                        measurementHyperbola.add(new double[]{ltln.getLat(), ltln.getLng()});
                    }
                    this.geoMission.hyperbolasToShow.add(obs.getId());
                    obs.setHyperbolaGeometry(measurementHyperbola);
                }
                else {
                    log.debug("Not adding hyperbola measurement lines");
                }
            }

            /* AOA MEASUREMENT */
            if (obs.getObservationType().equals(ObservationType.aoa)) {
                List<double[]> measurementLine = new ArrayList<double[]>();
                double b = obs.getY() - Math.tan(obs.getMeas())*obs.getX();
                double fromVal=0; double toVal=0;
                double x_run = Math.abs(Math.cos(obs.getMeas()))*5000;
                if (obs.getMeas()>Math.PI/2 && obs.getMeas()<3*Math.PI/2) { // negative-x plane projection
                    fromVal=-x_run; toVal=0;
                }
                else { // positive-x plane projection
                    fromVal=0; toVal=x_run;
                }

                for (double t = obs.getX()+fromVal; t<= obs.getX()+toVal; t += 100) {
                    double y = Math.tan(obs.getMeas())*t + b;
                    UTMRef utmMeas = new UTMRef(t, y, this.geoMission.getLatZone(), this.geoMission.getLonZone());
                    LatLng ltln = utmMeas.toLatLng();
                    double[] measPoint = {ltln.getLat(), ltln.getLng()};
                    measurementLine.add(measPoint);
                }
                this.geoMission.linesToShow.add(obs.getId());
                obs.setLineGeometry(measurementLine);
            }
        }

        /* Update the live observations - if a 'tracking' mission type */
        if (computeProcessor !=null && computeProcessor.isRunning()) {
            log.trace("Algorithm was running, will update observations list for tracking mode runs only");
            if (this.geoMission.getMissionMode().equals(MissionMode.track)) {
                // TEMP REMOVED TO FIX BUG
//                log.trace("Setting OBSERVATIONS in the filter, new size: "+this.geoMission.observations.size());
//                computeProcessor.setObservations(this.geoMission.observations);
            }
            else {
                log.debug("Not adding this OBSERVATION to filter since is configured to produce a single FIX, run again with different observations");
            }
        }
        else {
            log.debug("Algorithm was not running, observation will be available for future runs");
        }
    }

    public void stop() throws Exception {
        computeProcessor.stopThread();
    }

    /* For Tracker - start process and continually add new observations (one per asset), monitor result in result() callback */
    /* For Fixer - add observations (one per asset) then start, monitor output in result() callback */
    public ComputeResults start() throws Exception {
        Iterator it = this.geoMission.observations.values().iterator();
        if (!it.hasNext() && this.geoMission.getMissionMode().equals(MissionMode.fix)) {
            throw new ConfigurationException("There were no observations, couldn't start the process");
        }


        // TODO, Change this to Callable/Future Task ???
        //
        //     i.e. instead of implements runnable, implements Callable<RealVector>   (i.e. to return Xk matrix)
        // class CallableTask implements Callable<String>
        //  @Override
//        public String call() throws Exception {
//            System.out.println("Executing call() !!!");
//            return "success";
//        }
        //
        // REPLACE: computeProc = new ComputeProc...
        // WITH:
        // FutureTask<RealVector> computeProc = new FutureTask<>(new ComputeProcessor());
//        computeProc.run();
//        try {
//            RealVector result = future.get();
//            System.out.println("Result="+result);
//        } catch (InterruptedException | ExecutionException e) {
//            System.out.println("EXCEPTION!!!");
//            e.printStackTrace();
//        }

        computeProcessor = new ComputeProcessor(this.actionListener, this, this.geoMission.observations, this.geoMission);
        FutureTask<ComputeResults> future = new FutureTask<ComputeResults>(computeProcessor);
        future.run();

        try {
            ComputeResults results = future.get();
            log.debug("Result: "+results.toString());
            return results;
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public GeoMission getGeoMission() {
        return geoMission;
    }

    /* ADDED for SNET, configured for single target */
    public void reconfigureTarget(Target target) throws Exception {
        this.geoMission.setTarget(target);

        EfusionValidator.validateTarget(target);
    }

    // TEMPORARY hold - may need to also reinitialise the state matrices based on number of targets
    public void reconfigureTargets(Map<String,Target> targets) throws Exception {
        log.debug("Reconfiguring with # targets: "+targets.size());
        this.geoMission.setTargets(targets);

        EfusionValidator.validateTargets(targets.values());

//        // This needs to include targets even though no observations may be present?
//        // Determine targets requiring estimation - extract from set of observations
//        uniqueObservedTargets = new HashSet<String>();
//        log.debug("# active observations: "+this.observations.size());
//        for (Observation obs : this.observations.values()) {
//
//            //1. determine all unique targets, then set the size of relevant matrices
//            //2. index the state indexes in the stateIndexMap
//            uniqueObservedTargets.add(obs.getTargetId());
//            activeTargets.put(obs.getTargetId(), new Target(obs.getTargetId(), "ARB-NAME"));
//            log.debug("Processing observation: "+obs.toString());
//            if (obs.getTargetId_b()!=null) {
//                uniqueObservedTargets.add(obs.getTargetId_b());
//                activeTargets.put(obs.getTargetId_b(), new Target(obs.getTargetId_b(),"ARB-NAME"));
//                log.debug("Adding (secondary) target to target set: "+obs.getTargetId_b());
//            }
//            log.debug("Adding target to target set: "+obs.getTargetId());
//        }
//
//        //TODO, need to merge/remove targets, maintain an original set in geoMission.setTargets() which has lat/lon estimates updated to
//        // TODO, here need to dynamically create the targets array
//
//        if (this.geoMission.getTargets()==null || this.geoMission.getTargets().isEmpty()) {
//            log.debug("No target list set, initialising with # active targets: "+activeTargets.size());
//            this.geoMission.setTargets(activeTargets);
//        }
//        else {
//            log.debug("Merging with # existing targets: " + this.geoMission.getTargets().size());
//
//            for (Target target : this.geoMission.getTargets().values()) {
//
//                if (uniqueObservedTargets.contains(target.getId())) {
//                    // retain existing geoMissionTarget
//                    uniqueObservedTargets.remove(target.getId());
//                }
//            }
//
//            // for each remaining unique observed target, add it
//            for (String uniqueObservedTargetId : uniqueObservedTargets) {
//                this.geoMission.getTargets().put(uniqueObservedTargetId, new Target(uniqueObservedTargetId, "ARB-NEW-TGT-NAME"));
//            }
//            // Now
//            log.debug("Merged target list with missing targets, size now: "+this.geoMission.getTargets().size());
//        }
    }

    public void configure(GeoMission geoMission) throws Exception {
        this.geoMission = geoMission;

        EfusionValidator.validate(geoMission);

//        /* Uses defaults - overridden by some implementations (required for pur tdoa processing) */
//        geoMission.setFilterProcessNoise(new double[][]{{0.01, 0, 0, 0}, {0, 0.01 ,0, 0}, {0, 0, 0.01, 0}, {0, 0, 0 ,0.01}});    -- MOVED IN NAV USE CASE TO SPT DYNAMIC STATE SIZES, MOVED TO setObservations in computeProcessor

        Properties properties = new Properties();
        String appConfigPath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "application.properties";
        try {
            properties.load(new FileInputStream(appConfigPath));
            this.geoMission.setProperties(properties);
        }
        catch(IOException ioe) {
            log.error(ioe.getMessage());
            ioe.printStackTrace();
            log.error("Error reading application properties");
            throw new ConfigurationException("Trouble loading common application properties, reinstall the application");
        }

        if (geoMission.getOutputKml()) {
            log.debug("Creating new kml output file as: "+ properties.getProperty("working.directory")+"output/"+geoMission.getOutputKmlFilename());
            File kmlOutput = new File(properties.getProperty("working.directory")+"output/"+geoMission.getOutputKmlFilename());
            kmlOutput.createNewFile();
        }

        if (geoMission.getOutputFilterState()) {
            log.debug("Creating new kml output file as: "+ properties.getProperty("working.directory")+"output/"+geoMission.getOutputFilterStateKmlFilename());
            File kmlFilterStateOutput = new File(properties.getProperty("working.directory")+"output/"+geoMission.getOutputFilterStateKmlFilename());
            kmlFilterStateOutput.createNewFile();
        }

        /* Extract results dispatch period */
        if (geoMission.getDispatchResultsPeriod()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.dispatch_results_period") != null && !geoMission.getProperties().getProperty("ekf.filter.default.dispatch_results_period").isEmpty()) {
                geoMission.setDispatchResultsPeriod(new Long(properties.getProperty("ekf.filter.default.dispatch_results_period")));
            }
            else {
                throw new ConfigurationException("No dispatch results period specified");
            }
        }

        /* Extract throttle setting - NULL is allowed */
        if (geoMission.getFilterThrottle()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.throttle") != null) {
                if (geoMission.getProperties().getProperty("ekf.filter.default.throttle").isEmpty()) {
                    geoMission.setFilterThrottle(null);
                }
                else {
                    geoMission.setFilterThrottle(Long.parseLong(geoMission.getProperties().getProperty("ekf.filter.default.throttle")));
                }
            }
        }

        /* Extract convergence threshold setting */
        if (geoMission.getFilterConvergenceResidualThreshold()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.convergence_residual_threshold") != null && !geoMission.getProperties().getProperty("ekf.filter.default.convergence_residual_threshold").isEmpty()) {
                geoMission.setFilterConvergenceResidualThreshold(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.convergence_residual_threshold")));
            }
            else {
                throw new ConfigurationException("No convergence threshold specified");
            }
        }

        /* Extract dispatch threshold setting */
        if (geoMission.getFilterDispatchResidualThreshold()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.dispatch_residual_threshold") != null && !geoMission.getProperties().getProperty("ekf.filter.default.dispatch_residual_threshold").isEmpty()) {
                geoMission.setFilterDispatchResidualThreshold(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.dispatch_residual_threshold")));
            }
            else {
                throw new ConfigurationException("No dispatch threshold specified");
            }
        }

        /* Extract filter measurement error setting */
        if (geoMission.getFilterMeasurementError()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.measurement.error") != null && !geoMission.getProperties().getProperty("ekf.filter.default.measurement.error").isEmpty()) {
                geoMission.setFilterMeasurementError(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.measurement.error")));
            }
            else {
                throw new ConfigurationException("No filter measurement error specified");
            }
        }

        /* Extract filter bias settings */
        if (geoMission.getFilterAOABias()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.aoa.bias") != null && !geoMission.getProperties().getProperty("ekf.filter.default.aoa.bias").isEmpty()) {
                geoMission.setFilterAOABias(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.aoa.bias")));
            }
            else {
                throw new ConfigurationException("No filter aoa bias specified");
            }
        }

        /* Extract filter bias settings */
        if (geoMission.getFilterTDOABias()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.tdoa.bias") != null && !geoMission.getProperties().getProperty("ekf.filter.default.tdoa.bias").isEmpty()) {
                geoMission.setFilterTDOABias(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.tdoa.bias")));
            }
            else {
                throw new ConfigurationException("No filter tdoa bias specified");
            }
        }

        /* Extract filter bias settings */
        if (geoMission.getFilterRangeBias()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.range.bias") != null && !geoMission.getProperties().getProperty("ekf.filter.default.range.bias").isEmpty()) {
                geoMission.setFilterRangeBias(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.range.bias")));
            }
            else {
                throw new ConfigurationException("No filter range bias specified");
            }
        }
    }

    @Override
    public void result(String geoId, String target_id, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
        log.debug("Result Received at Process Manager: "+"Result -> GeoId: "+geoId+", TargetId: "+target_id+", Lat: "+lat+", Lon: "+lon+", CEP major: "+cep_elp_maj+", CEP minor: "+cep_elp_min+", CEP rotation: "+cep_elp_rot);
        this.resultBuffer.put(target_id, new GeoResult(geoId,target_id,lat,lon,cep_elp_maj,cep_elp_min,cep_elp_rot));
    }

    @Override
    public void result(ComputeResults computeResults) {
        log.debug("Result Received at Process Manager: "+"Result -> GeoId: "+computeResults.getGeoId()+", Lat: "+computeResults.getGeolocationResult().getLat()+", Lon: "+computeResults.getGeolocationResult().getLon()+", CEP major: "+computeResults.getGeolocationResult().getElp_long()+", CEP minor: "+computeResults.getGeolocationResult().getElp_short()+", CEP rotation: "+computeResults.getGeolocationResult().getElp_rot());
        log.warn("WARNING, not adding to results buffer in Process Manager REVISIT LATER");
        //this.resultBuffer.put(target_id, new GeoResult(geoId,target_id,lat,lon,cep_elp_maj,cep_elp_min,cep_elp_rot));
    }
}