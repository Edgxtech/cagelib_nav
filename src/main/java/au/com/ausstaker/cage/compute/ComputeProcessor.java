package au.com.ausstaker.cage.compute;

import au.com.ausstaker.cage.EfusionListener;
import au.com.ausstaker.cage.model.*;
import au.com.ausstaker.cage.util.Helpers;
import au.com.ausstaker.cage.util.KmlFileHelpers;
import au.com.ausstaker.cage.util.KmlFileStaticHelpers;
import org.apache.commons.math3.linear.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extended Kalman Filter Fusion Processor
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public class ComputeProcessor implements Callable<ComputeResults> {

    private static final Logger log = LoggerFactory.getLogger(ComputeProcessor.class);

    private EfusionListener efusionListener; // push results to user client

    private EfusionListener internalListener; // to push results to processListener for buffering

    private GeoMission geoMission;

    Map<Long, Observation> staged_observations = new ConcurrentHashMap<Long,Observation>();

    Map<Long,Observation> observations = new ConcurrentHashMap<Long,Observation>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    double[][] ThiData = { {1,0}, {0,1}};
    RealMatrix Thi = new Array2DRowRealMatrix(ThiData);

    double[][] controlData = { {0}, {0}};
    RealMatrix B = new Array2DRowRealMatrix(controlData);

    double[][] initCovarData = {{1, 0}, {0, 1}};
    RealMatrix Pinit = new Array2DRowRealMatrix(initCovarData);

    RealMatrix Qu;
    RealMatrix Rk;

    RealVector Xk;
    RealMatrix Pk;

    double[] innovd = {0,0};
    RealVector innov;

    double[][] P_innovd = {{0,0}, {0,0}};
    RealMatrix P_innov;

    double[][] eyeData = {{1,0}, {0,1}};
    RealMatrix eye = new Array2DRowRealMatrix(eyeData);

    RealMatrix H;
    double xk;
    double yk;
    RealMatrix K;

    KmlFileHelpers kmlFileHelpers = null;

    Map<String,Integer[]> stateIndexMap = new HashMap<String,Integer[]>();

    /*
     * Create processor for the given config, observations and client implemented listener
     */
    public ComputeProcessor(EfusionListener efusionListener, EfusionListener internalListener, Map<Long,Observation> observations, GeoMission geoMission)
    {
        this.efusionListener = efusionListener;
        this.geoMission = geoMission;
        this.internalListener = internalListener;

        setObservations(observations);
        initialiseFilter();
    }

    public void initialiseFilter() {

        // Determine an initial guess
        double[] start_x_y;
        if (this.geoMission.getFilterUseSpecificInitialCondition()) {
            //double[] init_utm = Helpers.convertLatLngToUtmNthingEasting(this.geoMission.getFilterSpecificInitialLat(), this.geoMission.getFilterSpecificInitialLon());
            double[] init_utm = Helpers.convertLatLngToUtmNthingEastingSpecificZone(this.geoMission.getFilterSpecificInitialLat(), this.geoMission.getFilterSpecificInitialLon(), this.geoMission.getLatZone(), this.geoMission.getLonZone());
            start_x_y = new double[]{init_utm[1], init_utm[0]};
        }
        else {
            List<Asset> assetList = new ArrayList<Asset>(this.geoMission.getAssets().values());
            log.debug("Initialising filter, number of assets registered through observations: " + this.geoMission.getAssets().values().size());
            if (assetList.size() > 1) {
                Random rand = new Random();
                Asset randAssetA = assetList.get(rand.nextInt(assetList.size()));
                assetList.remove(randAssetA);
                Asset randAssetB = assetList.get(rand.nextInt(assetList.size()));
                //log.debug("Finding rudimentary start point between two random observations: " + randAssetA.getId() + "," + randAssetB.getId());
                start_x_y = findRudimentaryStartPoint(randAssetA, randAssetB, (Math.random() - 0.5) * 100000);
                log.debug("Using RANDOM initial condition: near asset(s) ['" + randAssetA.getId() + "' & '" + randAssetB.getId() + "']: " + start_x_y[1] + ", " + start_x_y[0]);
            } else {
                Asset asset = assetList.get(0);
                //double[] asset_utm = Helpers.convertLatLngToUtmNthingEasting(asset.getCurrent_loc()[0], asset.getCurrent_loc()[1]);
                double[] asset_utm = Helpers.convertLatLngToUtmNthingEastingSpecificZone(asset.getCurrent_loc()[0], asset.getCurrent_loc()[1], this.geoMission.getLatZone(), this.geoMission.getLonZone());
                start_x_y = new double[]{asset_utm[1] + 5000, asset_utm[0] - 5000};
                log.debug("Using RANDOM initial condition: near asset [" + asset.getId() + "]: " + start_x_y[1] + ", " + start_x_y[0]);
            }
        }
        log.debug("Filter start state: "+start_x_y[0]+","+start_x_y[1]);

        RealVector Xinit = new ArrayRealVector(start_x_y);

        // Set initial process noise data
        /* Uses defaults - overridden by some implementations (required for pure tdoa processing) */
        geoMission.setFilterProcessNoise(eye.scalarMultiply(0.01).getData()); //setFilterProcessNoise(new double[][]{{0.01, 0, 0, 0}, {0, 0.01 ,0, 0}, {0, 0, 0.01, 0}, {0, 0, 0 ,0.01}});
        log.debug("Set filter process noise data as: "+eye.scalarMultiply(0.01));

        Xk = Xinit;
        Pk = Pinit.scalarMultiply(0.01);
        log.debug("Running Fix Execution with Init State: "+Xk);
        log.debug("Running Fix Execution with Init State Covariance: "+Pk);

        log.trace("Initialising Stage Observations as current observations, #: "+this.geoMission.observations.size());
        setStaged_observations(this.geoMission.observations);

        double[][] measurementNoiseData = {{geoMission.getFilterMeasurementError()}};
        Rk = new Array2DRowRealMatrix(measurementNoiseData);

        double[][] procNoiseData = geoMission.getFilterProcessNoise();
        Qu = new Array2DRowRealMatrix(procNoiseData);
    }

    public void setObservations(Map<Long, Observation> observations) {
        /// NOTE, this cannot have any new targets to process, since it will affect the size of state matrices
        // then this step "int[] stateIndexes = stateIndexMap.get(obs.getTargetId());" will be null
        Comparator<Map.Entry<Long, Observation>> valueComparator = new Comparator<Map.Entry<Long, Observation>>() {
            @Override
            public int compare(Map.Entry<Long, Observation> e1, Map.Entry<Long, Observation> e2) {
                ObservationType v1 = e1.getValue().getObservationType();
                ObservationType v2 = e2.getValue().getObservationType();
                return v1.compareTo(v2);
            }
        };

        List<Map.Entry<Long, Observation>> listOfEntries = new ArrayList<Map.Entry<Long, Observation>>(observations.entrySet());
        Collections.sort(listOfEntries, valueComparator);
        LinkedHashMap<Long, Observation> sortedByValue = new LinkedHashMap<Long, Observation>(listOfEntries.size());
        for(Map.Entry<Long, Observation> entry : listOfEntries){
            sortedByValue.put(entry.getKey(), entry.getValue());
        }
        this.observations = sortedByValue;
    }

    @Override
    public ComputeResults call()
    {
        log.info("Running for # observations: "+observations.size());
        if (observations.size()==0) {
            log.info("No observations returning");
            return null;
        }

        running.set(true);

        // Initialise this just so the starting results dispatch functions
        innov = new ArrayRealVector(innovd);
        P_innov = new Array2DRowRealMatrix(P_innovd);

        // TODO, replace this with new dispatch method???
        //dispatchResult(Xk);
        GeolocationResult geolocationResult = summariseResult(Xk, GeolocationResultStatus.in_progress, null);
        ComputeResults computeResults = new ComputeResults();
        computeResults.setGeolocationResult(geolocationResult);
        computeResults.setGeoId(this.geoMission.getGeoId());
        /* Dispatch Result to listeners */
        dispatchResult(computeResults);

        Vector<FilterObservationDTO> filterObservationDTOs = new Vector<FilterObservationDTO>();

        FilterStateDTO filterStateDTO = new FilterStateDTO();

        long startTime = Calendar.getInstance().getTimeInMillis();
        long universalStartTime = startTime;

        if (this.geoMission.getOutputFilterState()) {
            kmlFileHelpers = new KmlFileHelpers();
            kmlFileHelpers.provisionFilterStateExport();
            log.debug("Provisioned filter state export");
        }
        int filterStateExportCounter = 0;

        GeolocationResultStatus status = GeolocationResultStatus.in_progress;
        String status_message = null;

        while(true) {
            if (!running.get()) {
                log.debug("Thread was stopped");
                break;
            }

            if (this.geoMission.getFilterThrottle() != null) {
                try {
                    log.trace("Throttling for miliseconds: " + this.geoMission.getFilterThrottle());
                    Thread.sleep(this.geoMission.getFilterThrottle());
                } catch (InterruptedException ie) {
                    log.warn("Error throttling filter");
                }
            }

            Xk = Thi.operate(Xk);

            Pk = (Thi.multiply(Pk).multiply(Thi.transpose())).add(Qu);

//            /* reinitialise various collections */
            innov = new ArrayRealVector(innovd);
            P_innov = new Array2DRowRealMatrix(P_innovd);
            filterObservationDTOs.removeAllElements();
            RealVector nonAoaNextState = null;
            Iterator obsIterator = this.observations.values().iterator();

            log.trace("Innovating with # Observations: "+this.observations.size());

            while (obsIterator.hasNext()) {

                Observation obs = (Observation) obsIterator.next();

                    xk = Xk.getEntry(0);
                    yk = Xk.getEntry(1);
                    // REMOVED MULTI TGT FOR SNET

                    double f_est = 0.0;
                    double d = 0.0;
                    H = null;

                    if (obs.getObservationType().equals(ObservationType.range)) {

                        H = recalculateH(obs.getX(), obs.getY(), xk, yk);

                        f_est = Math.sqrt(Math.pow((obs.getX() - xk), 2) + Math.pow(obs.getY() - yk, 2));

                        d = obs.getMeas();
                        log.trace("RANGE innovation: " + f_est + ", vs d: " + d);

                    } else if (obs.getObservationType().equals(ObservationType.aoa)) {

                        H = recalculateH_AOA(obs.getX(), obs.getY(), xk, yk);

                        f_est = Math.atan((obs.getY() - yk)/(obs.getX() - xk))*180/Math.PI;

                        if (xk<obs.getX()) {
                            f_est = f_est + 180;
                        }

                        if (yk<obs.getY() && xk>=obs.getX()) {
                            f_est = 360 - Math.abs(f_est);
                        }

                        d = obs.getMeas() * 180 / Math.PI;
                        //log.debug("AOA innovation: " + f_est + ", vs d: " + d);
                    }

                    RealMatrix toInvert = (H.multiply(Pk).multiply(H.transpose()).add(Rk));
                    RealMatrix Inverse = (new LUDecomposition(toInvert)).getSolver().getInverse();

                    K = Pk.multiply(H.transpose()).multiply(Inverse); // DEPRECATED.scalarMultiply(this.geoMission.getFilterRangeBias());

                    double rk = d - f_est;

                    /* '360-0 Conundrum' adjustment - REMOVED to SNIPPETs */

                    RealVector innov_ = K.scalarMultiply(rk).getColumnVector(0);

                    innov = innov_.add(innov);

                    P_innov = K.multiply(H).multiply(Pk).add(P_innov);

                    log.trace("Adding observation for tracking: "+obs.toString());
                    filterObservationDTOs.add(new FilterObservationDTO(obs, f_est, innov_));
                }

                Xk = Xk.add(innov);
                Pk = (eye.multiply(Pk)).subtract(P_innov);

                /* Export filter state - development debugging */
                if (this.geoMission.getOutputFilterState()) {
                    filterStateExportCounter++;
                    if (filterStateExportCounter == 10) {
                        // Only if it is changing significantly
                        double residual = Math.abs(innov.getEntry(0)) + Math.abs(innov.getEntry(1));
                        if (residual > 0.5) {
                            filterStateDTO.setFilterObservationDTOs(filterObservationDTOs);
                            filterStateDTO.setXk(Xk);
                            kmlFileHelpers.exportAdditionalFilterState(this.geoMission, filterStateDTO, residual);
                            filterStateExportCounter = 0;
                        }
                    }
                }

            /* Export Result */
            if ((Calendar.getInstance().getTimeInMillis() - startTime) > this.geoMission.getDispatchResultsPeriod()) {

                Target target = this.geoMission.getTarget();

                /* A measure of residual changes the filter intends to make - using delta_x/y innovation data */
                double residual = Math.abs((innov.getEntry(0) + innov.getEntry(1)));
                // ADDED in _nav: Normalise the residual measurement
                residual = residual / this.observations.size();

                // ADDED in _nav: Long-long time cut-off
                if ((Calendar.getInstance().getTimeInMillis() - universalStartTime) > 7500) {
                    log.debug("Filter has run for max allowable time, breaking execution. The geometry did not lend itself to a geolocation result");
                    status=GeolocationResultStatus.hung;
                    status_message="The compute process hung after processing for long time, if the residual is large, this indicates that the geometry of observations do not lend themselves to a single geolocation point and the result is not useable";
                    break;
                }

                log.trace("Residual: "+residual+", vs filterDispatchResidualError: "+this.geoMission.getFilterDispatchResidualThreshold());
                // NOTE: if residual threshold not met here, will be stuck in loop
                if (residual < this.geoMission.getFilterDispatchResidualThreshold()) {
                    log.debug("Measurement Error: "+geoMission.getFilterMeasurementError());
                    log.debug("target: [" + target.getId() + "]: Dispatching Result From # Observations: " + this.observations.size());
                    log.debug("target: [" + target.getId() + "]: Residual Movements (Normalised): " + residual);
                    log.debug("target: [" + target.getId() + "]: Residual Innovation: " + innov);
                    log.debug("target: [" + target.getId() + "]: Covariance: " + Pk);

                    if (log.isDebugEnabled()) {
                        log.debug("Printing observation utilisation data for # observations: "+filterObservationDTOs.size());
                        for (FilterObservationDTO obs_state : filterObservationDTOs) {
                            double f_est_adj = obs_state.getF_est();
                            if (obs_state.getObs().getObservationType().equals(ObservationType.tdoa)) {
                                f_est_adj = f_est_adj / Helpers.SPEED_OF_LIGHT;
                            } else if (obs_state.getObs().getObservationType().equals(ObservationType.aoa)) {
                                f_est_adj = f_est_adj * Math.PI / 180;
                            }
                            log.debug("Observation utilisation: asset:" + obs_state.getObs().getAssetId() +", Target: "+obs_state.getObs().getTargetId()+ ", type: " + obs_state.getObs().getObservationType().name() + ", f_est(adj): " + f_est_adj + ",d: " + obs_state.getObs().getMeas() + ", innov: " + obs_state.getInnov());
                        }
                    }

                    startTime = Calendar.getInstance().getTimeInMillis();

                    geolocationResult = summariseResult(Xk, GeolocationResultStatus.in_progress, null);
                    computeResults = new ComputeResults();
                    computeResults.setGeolocationResult(geolocationResult);
                    computeResults.setGeoId(this.geoMission.getGeoId());

                    /* Dispatch Result to listeners */
                    dispatchResult(computeResults);

                    // If all targets are below threshold then break
                    if (geoMission.getMissionMode().equals(MissionMode.fix)) {

                        /// DEVING
                        log.debug("Residual: "+residual+", vs filterMeasurementError: "+this.geoMission.getFilterMeasurementError());
                        if (residual < this.geoMission.getFilterMeasurementError()/10) {

                            if (residual < this.geoMission.getFilterConvergenceResidualThreshold()) {  /// ORIGINAL

                                log.debug("Exiting since this is a FIX Mode run and filter has converged to threshold");
                                status = GeolocationResultStatus.ok;
                                running.set(false);
                                break;
                            }
                        }
                    } else {
                        log.debug("This is a Tracking mode run, using latest observations (as held in staging) and continuing...");

                            /* Resynch latest observations, and reinitialise with current state estimate */
                        log.debug("# Staged observations: " + this.staged_observations.size());
                        setObservations(this.staged_observations);
                    }
                } else {
                    log.trace("Residual not low enough to export result: " + residual);
                }
            }
        }

        geolocationResult = summariseResult(Xk, status, status_message);
        computeResults = new ComputeResults();
        computeResults.setGeolocationResult(geolocationResult);
        computeResults.setGeoId(this.geoMission.getGeoId());
        /* Dispatch Result to listeners - DEPRECATED */
        //dispatchResult(computeResults);
        return computeResults;
    }

    public RealMatrix recalculateH(double x_rssi, double y_rssi, double Xk1, double Xk2) {

        double R1 = Math.sqrt(Math.pow((x_rssi-Xk1),2) + Math.pow(y_rssi-Xk2,2));

        double dfdx = -(x_rssi-Xk1)/R1;
        double dfdy = -(y_rssi-Xk2)/R1;

        double[][] jacobianData = {{dfdx, dfdy}};
        RealMatrix H = new Array2DRowRealMatrix(jacobianData);
        return H;
    }

    public RealMatrix recalculateH_TDOA(double x, double y, double x2, double y2, double Xk1, double Xk2) {

        double R1 = Math.sqrt(Math.pow((x-Xk1),2) + Math.pow(y-Xk2,2));
        double R2 = Math.sqrt(Math.pow((x2-Xk1),2) + Math.pow(y2-Xk2,2));

        double dfdx = (-x+Xk1)/R1 - (-x2+Xk1)/R2;
        double dfdy = (-y+Xk2)/R1 - (-y2+Xk2)/R2;

        double[][] jacobianData = {{dfdx, dfdy}};
        RealMatrix H = new Array2DRowRealMatrix(jacobianData);
        return H;
    }

    public RealMatrix recalculateH_AOA(double x, double y, double Xk1, double Xk2) {

        double R1 = Math.sqrt(Math.pow((x-Xk1),2) + Math.pow((y-Xk2),2)); // Note: better performance using sqrt

        double dfdx = (y-Xk2)/R1;  // Note d/d"x" = "y - y_est"/..... on purpose linearisation
        double dfdy = -(x-Xk1)/R1;

        double[][] jacobianData = {{dfdx, dfdy}};
        RealMatrix H = new Array2DRowRealMatrix(jacobianData);
        return H;
    }

    public double[] findRudimentaryStartPoint(Asset asset_a, Asset asset_b, double addition) {
        double x_init=0; double y_init=0;
        double[] asset_a_utm = Helpers.convertLatLngToUtmNthingEastingSpecificZone(asset_a.getCurrent_loc()[0],asset_a.getCurrent_loc()[1], this.geoMission.getLatZone(), this.geoMission.getLonZone());
        double[] asset_b_utm = Helpers.convertLatLngToUtmNthingEastingSpecificZone(asset_b.getCurrent_loc()[0],asset_b.getCurrent_loc()[1], this.geoMission.getLatZone(), this.geoMission.getLonZone());
        if (asset_b == null) {
            x_init = asset_a_utm[1] + addition;
            y_init = asset_a_utm[0] - addition;
        }
        else {
            x_init = asset_a_utm[1];
            y_init = asset_a_utm[0];
            double x_n = asset_b_utm[1];
            double y_n = asset_b_utm[0];
            x_init = x_init + (x_init - x_n) / 2;
            y_init = y_init + (y_init - y_n) / 2;
        }
        return new double[]{x_init,y_init};
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public void stopThread() {
        this.running.set(false);
    }

    public double[][] getCurrentEstimatesForTargets(int target_id_a, int target_id_b) {
        Integer[] indexes_a = stateIndexMap.get(target_id_a);
        Integer[] indexes_b = stateIndexMap.get(target_id_b);
        double[] utm_x_y_a = new double[]{Xk.getEntry(indexes_a[0]), Xk.getEntry(indexes_a[1])};
        double[] utm_x_y_b = new double[]{Xk.getEntry(indexes_a[0]), Xk.getEntry(indexes_a[1])};
        return new double[][]{utm_x_y_a,utm_x_y_b};
    }

    public synchronized void setStaged_observations(Map<Long, Observation> staged_observations) {
        this.staged_observations = staged_observations;
    }

    public GeolocationResult summariseResult(RealVector Xk, GeolocationResultStatus status, String status_message) {

        log.debug("Summarising Result, using latzone: "+this.geoMission.getLatZone()+", lonzone: "+this.geoMission.getLonZone());
        log.debug("Summarising Result, computed UTM: "+Xk.getEntry(0)+","+Xk.getEntry(1));
        double[] latLon = Helpers.convertUtmNthingEastingToLatLng(Xk.getEntry(0),Xk.getEntry(1), this.geoMission.getLatZone(), this.geoMission.getLonZone());
        log.debug("Summarising Result, computed lat: "+latLon[0]+", Lon: "+latLon[1]);

        /* Compute probability ELP */
        double[][] covMatrix=new double[][]{{Pk.getEntry(0,0),Pk.getEntry(0,1)},{Pk.getEntry(1,0),Pk.getEntry(1,1)}};
        log.debug(Pk.toString());
        double[] evalues = Helpers.getEigenvalues(covMatrix);
        double largestEvalue = Math.max(evalues[0],evalues[1]);
        double smallestEvalue = Math.min(evalues[0],evalues[1]);
        log.debug("Evalues: "+evalues);
        double[] evector = Helpers.getEigenvector(covMatrix, largestEvalue);
        /* Alternative is to use this
         *  RealMatrix J2 = new Array2DRowRealMatrix(covMatrix);
            EigenDecomposition eig = new EigenDecomposition(J2);
            double[] evalues = eig.getRealEigenvalues();
            log.debug("#4 E-values: "+evalues[0]+","+evalues[1]);
            log.debug("#1 E-vector: "+evector[0]+","+evector[1]);*/
        double rot = Math.atan(evector[1] / evector[0]);
        /* This angle is between -pi -> pi, adjust 0->2pi */
        if (rot<0)
            rot = rot + 2*Math.PI;
        /* Ch-square distribution for two degrees freedom: 1.39 equiv 50% (i.e. CEP), 5.991 equiv 95% C.I, 4.605 equiv 90% C.I, 9.210 equiv 99% C.I */
        double half_major_axis_length = Math.sqrt(largestEvalue)*1.39; // Orig used: 2*Math.sqrt(9.210*largestEvalue);
        double half_minor_axis_length = Math.sqrt(smallestEvalue)*1.39;
        this.geoMission.getTarget().setElp_major(half_major_axis_length*10000); /* UTM -> [m]: X10^4 */
        this.geoMission.getTarget().setElp_minor(half_minor_axis_length*10000);
        this.geoMission.getTarget().setElp_rot(rot);
        log.debug("Half minor axis: "+half_minor_axis_length);
        log.debug("Half major axis: "+half_major_axis_length);

        /* A measure of the aggreance by all measurements */
        //double residual_rk = findResidualRk(filterExecution.getFilterObservationDTOs());

        /* A measure of residual changes the filter intends to make */
        double residual = Math.abs(innov.getEntry(0)) + Math.abs(innov.getEntry(1));

        log.debug("Dispatching Result From # Observations: " + this.observations.size());
        log.debug("Result: "+latLon[0]+","+latLon[1]);
        log.debug("Residual Movements: "+residual);
        //log.debug("Residual Measurement Delta: "+residual_rk);
        log.debug("Residual Innovation: "+innov);
        log.debug("Covariance: "+Pk);

        GeolocationResult geolocationResult = new GeolocationResult();
        geolocationResult.setLat(latLon[0]);
        geolocationResult.setLon(latLon[1]);
        geolocationResult.setElp_long(half_major_axis_length*10000);
        geolocationResult.setElp_short(half_minor_axis_length*10000);
        geolocationResult.setElp_rot(rot);
        geolocationResult.setResidual(residual);
        geolocationResult.setStatus(status);
        geolocationResult.setStatus_message(status_message);
        //geolocationResult.setResidual_rk(residual_rk);

        return geolocationResult;
    }

    public void dispatchResult(ComputeResults computeResults) {

        this.geoMission.getTarget().setCurrent_loc(new double[]{computeResults.getGeolocationResult().getLat(),computeResults.getGeolocationResult().getLon()});
        this.efusionListener.result(computeResults);

        this.geoMission.setComputeResults(computeResults);

        if (this.geoMission.getOutputFilterState() && kmlFileHelpers !=null) {
            /* Write non-static file exports, includes filter state data stored in mem */
            kmlFileHelpers.writeCurrentExports(this.geoMission);
        }

        log.debug("In dispatchResult, should output kml?: "+this.geoMission.getOutputKml());
        if (this.geoMission.getOutputKml()) {
            KmlFileStaticHelpers.exportGeoMissionToKml(this.geoMission);
        }
    }
}
