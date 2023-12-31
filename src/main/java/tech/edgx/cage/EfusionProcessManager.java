package tech.edgx.cage;

import tech.edgx.cage.model.*;
import tech.edgx.cage.util.ConfigurationException;
import tech.edgx.cage.util.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.edgx.cage.compute.ComputeProcessor;
import tech.edgx.cage.compute.ComputeResults;
import tech.edgx.cage.util.EfusionValidator;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static java.util.stream.Collectors.toCollection;

/**
 * Geolocation fusion and tracking, using custom extended kalman filter implementation
 */
public class EfusionProcessManager implements Serializable, EfusionListener {

    private static final Logger log = LoggerFactory.getLogger(EfusionProcessManager.class);

    EfusionListener actionListener;

    GeoMission geoMission;

    ComputeProcessor computeProcessor;

    Map<String, GeoResult> resultBuffer = new HashMap<String,GeoResult>();

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

    public void addObservation(Observation observation) throws Exception {

        EfusionValidator.validate(observation);

        Asset asset = new Asset(observation.getAssetId(),new double[]{observation.getLat(),observation.getLon()});
        this.geoMission.getAssets().put(observation.getAssetId(),asset);

        /* Set previous measurement here, if this is a repeated measurement */
        if (this.getGeoMission().getObservations().get(observation.getId()) != null) {
            Observation prev_observation = this.getGeoMission().getObservations().get(observation.getId());
            if (prev_observation.getMeas()!=null) {
                log.trace("Setting previous observation for: "+prev_observation.getId()+", type: "+prev_observation.getObservationType().name()+", as: "+prev_observation.getMeas());
                observation.setMeas_prev(prev_observation.getMeas());
            }
        }

        /* Determine most prominent UTM zone across all assets, so a single UTM is used for computation */
        int lonZone;
        char latZone;
        if (this.geoMission.getAssets().size()==1) {
            log.debug("First UTM zone check, using first assets UTM zone");
            Object[] zones = Helpers.getUtmLatZoneLonZone(observation.getLat(), observation.getLon());
            lonZone = (int) zones[1];
            latZone = (char)zones[0];
        }
        else {
            List<Asset> assets = this.geoMission.getAssets().values().stream().collect(toCollection(ArrayList::new));
            lonZone = Helpers.getMostPopularLonZoneFromAssets(assets);
            latZone = Helpers.getMostPopularLatZoneFromAssets(assets);
        }
        log.debug("Most prominent Lon Zone: "+lonZone);
        log.debug("Most prominent Lat Zone: "+latZone);

        this.geoMission.setLatZone(latZone);
        this.geoMission.setLonZone(lonZone);

        /* Add the observation to in-memory collection */
        log.debug("Adding observation: "+observation.toString());
        this.geoMission.getObservations().put(observation.getId(), observation);

        /* FOR ALL existing observations recompute their UTM related properties with the new zone */
        log.debug("Readjusting #"+this.geoMission.getObservations().keySet().size()+" observations to new lat/lon zones");
        for (Observation obs : this.geoMission.getObservations().values()) {

            obs.setY_latZone(latZone);
            obs.setX_lonZone(lonZone);

            double[] utm_coords = Helpers.convertLatLngToUtmNthingEastingSpecificZone(obs.getLat(), obs.getLon(), latZone, lonZone);
            obs.setY(utm_coords[0]);
            obs.setX(utm_coords[1]);
            log.debug("Asset UTM loc:"+obs.getY()+","+obs.getX());

            if (this.geoMission.getShowMeas()) {
                /* RANGE MEASUREMENT */
                if (obs.getObservationType().equals(ObservationType.range)) {
                    List<double[]> measurementCircle = new ArrayList<double[]>();
                    for (double theta = (1 / 2) * Math.PI; theta <= (5 / 2) * Math.PI; theta += 0.2) {
                        UTMRef utmMeas = new UTMRef(obs.getMeas() * Math.cos(theta) + obs.getX(), obs.getMeas() * Math.sin(theta) + obs.getY(), obs.getY_latZone(), obs.getX_lonZone());
                        LatLng ltln = utmMeas.toLatLng();
                        double[] measPoint = {ltln.getLat(), ltln.getLng()};
                        measurementCircle.add(measPoint);
                    }
                    this.geoMission.circlesToShow.add(obs.getId());
                    obs.setCircleGeometry(measurementCircle);
                }

                /* TDOA MEASUREMENT - DEVELOPMENTAL CODE NOT USED */
                if (obs.getObservationType().equals(ObservationType.tdoa)) {
                    List<double[]> measurementHyperbola = new ArrayList<double[]>();

                    // Attempt to use the current target estimated locations. For the given observation target id's, check if there is a suitable state estimate available for those targets
                    GeoResult r_a = this.resultBuffer.get(obs.getTargetId());
                    GeoResult r_b = this.resultBuffer.get(obs.getTargetId_b());

                    if ((r_a!=null) && (r_b!=null)) {
                        log.debug("Using target position for TDOA plot 1/2: "+r_a.toString());
                        log.debug("Using target position for TDOA plot 2/2: "+r_b.toString());
                        double[] r_a_utm = Helpers.convertLatLngToUtmNthingEastingSpecificZone(r_a.getLat(), r_a.getLon(), obs.getY_latZone(), obs.getX_lonZone()); // RETURNS IN [NTHING===Y , EASTING===X]
                        double[] r_b_utm = Helpers.convertLatLngToUtmNthingEastingSpecificZone(r_b.getLat(), r_b.getLon(), obs.getY_latZone(), obs.getX_lonZone());

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
                            UTMRef utmMeas = new UTMRef(x, y, obs.getY_latZone(), obs.getX_lonZone());
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
                    if (obs.getMeas()>Math.PI/2 && obs.getMeas()<3*Math.PI/2) {
                        // negative-x plane projection
                        fromVal=-x_run; toVal=0;
                    }
                    else {
                        // positive-x plane projection
                        fromVal=0; toVal=x_run;
                    }

                    for (double t = obs.getX()+fromVal; t<= obs.getX()+toVal; t += 100) {
                        double y = Math.tan(obs.getMeas())*t + b;
                        UTMRef utmMeas = new UTMRef(t, y, obs.getY_latZone(), obs.getX_lonZone());
                        LatLng ltln = utmMeas.toLatLng();
                        double[] measPoint = {ltln.getLat(), ltln.getLng()};
                        measurementLine.add(measPoint);
                    }
                    this.geoMission.linesToShow.add(obs.getId());
                    obs.setLineGeometry(measurementLine);
                }
            }
        }

        log.debug("All observation zones (should be the same): ");
        for (Observation o : this.geoMission.getObservations().values()) {
            log.debug(o.getId()+"; lonZone: "+o.getX_lonZone()+", latZone: "+o.getY_latZone());
        }

        /* Update live observations - if a 'tracking' mission type */
        if (computeProcessor !=null && computeProcessor.isRunning()) {
            log.trace("Algorithm was running, will update observations list for tracking mode runs only");
            if (this.geoMission.getMissionMode().equals(MissionMode.track)) {
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

    public void reconfigureTarget(Target target) throws Exception {
        this.geoMission.setTarget(target);
        EfusionValidator.validateTarget(target);
    }

    public void configure(GeoMission geoMission) throws Exception {
        this.geoMission = geoMission;
        log.debug("Configuring GeoMission: "+geoMission.toString());
        EfusionValidator.validate(geoMission);
        /* Uses defaults - overridden by some implementations (required for pur tdoa processing) */
        Properties properties = new Properties();
        InputStream input = EfusionProcessManager.class.getClassLoader().getResourceAsStream("cage_nav.properties");

        if (input == null) {
            throw new ConfigurationException("Trouble loading common application properties, file was not found");
        }

        try {
            properties.load(input);
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
                //throw new ConfigurationException("No filter aoa bias specified");
                log.debug("No filter aoa bias specified, using none");
                geoMission.setFilterAOABias(1.0);
            }
        }

        /* Extract filter bias settings */
        if (geoMission.getFilterTDOABias()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.tdoa.bias") != null && !geoMission.getProperties().getProperty("ekf.filter.default.tdoa.bias").isEmpty()) {
                geoMission.setFilterTDOABias(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.tdoa.bias")));
            }
            else {
                //throw new ConfigurationException("No filter tdoa bias specified");
                log.debug("No filter aoa bias specified, using none");
                geoMission.setFilterTDOABias(1.0);
            }
        }

        /* Extract filter bias settings */
        if (geoMission.getFilterRangeBias()==null) {
            if (geoMission.getProperties().getProperty("ekf.filter.default.range.bias") != null && !geoMission.getProperties().getProperty("ekf.filter.default.range.bias").isEmpty()) {
                geoMission.setFilterRangeBias(Double.parseDouble(geoMission.getProperties().getProperty("ekf.filter.default.range.bias")));
            }
            else {
                //throw new ConfigurationException("No filter range bias specified");
                log.debug("No filter aoa bias specified, using none");
                geoMission.setFilterRangeBias(1.0);
            }
        }
    }

    @Override
    public void result(ComputeResults computeResults) {
        log.debug("Result Received at Process Manager: "+"Result -> GeoId: "+computeResults.getGeoId()+", Lat: "+computeResults.getGeolocationResult().getLat()+", Lon: "+computeResults.getGeolocationResult().getLon()+", CEP major: "+computeResults.getGeolocationResult().getElp_long()+", CEP minor: "+computeResults.getGeolocationResult().getElp_short()+", CEP rotation: "+computeResults.getGeolocationResult().getElp_rot());
        log.warn("WARNING, not adding to results buffer in Process Manager");
    }
}