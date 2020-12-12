package tech.tgo.cage.compute;

import org.apache.commons.math3.linear.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tgo.cage.EfusionListener;
import tech.tgo.cage.model.*;
import tech.tgo.cage.util.Helpers;
import tech.tgo.cage.util.KmlFileHelpers;
import tech.tgo.cage.util.KmlFileStaticHelpers;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extended Kalman Filter Fusion Processor
 * @author Timothy Edge (timmyedge)
 */
public class ComputeProcessor_MultiTargets_AL0 implements Callable<ComputeResults> {

    private static final Logger log = LoggerFactory.getLogger(ComputeProcessor_MultiTargets_AL0.class);

    private EfusionListener efusionListener; // push results to user client

    private EfusionListener internalListener; // to push results to processListener for buffering

    private GeoMission geoMission;

    Map<Long,Observation> staged_observations = new ConcurrentHashMap<Long,Observation>();

    Map<Long,Observation> observations = new ConcurrentHashMap<Long,Observation>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    RealMatrix Thi;
    RealMatrix Pinit;

    RealMatrix Qu;
    RealMatrix Rk;

    RealVector Xk;
    RealMatrix Pk;

    RealVector innov;
    RealMatrix P_innov;
    RealMatrix eye;

    RealMatrix H;
    double xk;
    double yk;
    RealMatrix K;

    KmlFileHelpers kmlFileHelpers = null;

    Map<String,Integer[]> stateIndexMap = new HashMap<String,Integer[]>();

    int matrice_size;
    int number_unique_targets;

    Set<String> uniqueObservedTargets;
    Map<String,Target> activeTargets = new HashMap<String,Target>();

    /*
     * Create processor for the given config, observations and client implemented listener
     */
    public ComputeProcessor_MultiTargets_AL0(EfusionListener efusionListener, EfusionListener internalListener, Map<Long,Observation> observations, GeoMission geoMission)
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
            double[] init_utm = Helpers.convertLatLngToUtmNthingEasting(this.geoMission.getFilterSpecificInitialLat(), this.geoMission.getFilterSpecificInitialLon());
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
                double[] asset_utm = Helpers.convertLatLngToUtmNthingEasting(asset.getCurrent_loc()[0], asset.getCurrent_loc()[1]);
                start_x_y = new double[]{asset_utm[1] + 5000, asset_utm[0] - 5000};
                log.debug("Using RANDOM initial condition: near asset [" + asset.getId() + "]: " + start_x_y[1] + ", " + start_x_y[0]);
            }
        }
        log.debug("Filter start state: "+start_x_y[0]+","+start_x_y[1]);


        // Rebuild the filters state based on number of observing targets

        // For each unique target, pad out the matrices
        // 1. For 1 target, Thi=4x4, 2, 8x8 ... etc...
        log.debug("Unique targets: "+this.geoMission.getTargets().size());
        number_unique_targets = this.geoMission.getTargets().size();
        matrice_size = number_unique_targets*4; // states * delta_states * number of targets

        // initialise Xinit [for reuse to re-initialise Xk]
        log.debug("Defining state vector, size: "+matrice_size);
        RealVector Xinit = new ArrayRealVector(matrice_size);

        //initialise Thi
        Thi = new Array2DRowRealMatrix(matrice_size, matrice_size);
        int i=0;
        for (Target target : this.geoMission.getTargets().values()) {
            // Create like this: 1 0 0 0 1 0 0 0; 0 1 0 0 0 1 0 0 ...
            Thi.setEntry(i,i,1);                             // x
            Thi.setEntry(i,i+(matrice_size/2),1);        // delta_x
            Thi.setEntry(i+1,i+1,1);                  // y
            Thi.setEntry(i+1,i+1+(matrice_size/2),1); // delta_y

            // Create like this: init_lat, init_lon, init_lat_b, init_lon_b, 1, 1, 1, 1
            Xinit.setEntry(i,start_x_y[0]); // x
            Xinit.setEntry(i+(matrice_size/2), 1); // delta_x
            Xinit.setEntry(i+1, start_x_y[1]); // y
            Xinit.setEntry(i+1+(matrice_size/2), 1); // delta_y

            stateIndexMap.put(target.getId(), new Integer[]{i, i+1});
            i=i+2;
        }
        log.debug("Set Thi as: "+Thi);

        Pinit = MatrixUtils.createRealIdentityMatrix(matrice_size);//new Array2DRowRealMatrix(matrice_size, matrice_size);
        MatrixUtils.createRealIdentityMatrix(matrice_size);

        eye = MatrixUtils.createRealIdentityMatrix(matrice_size); //new Array2DRowRealMatrix(matrice_size, matrice_size);

        log.debug("Set Eye as: "+eye);

        // Set initial process noise data
        /* Uses defaults - overridden by some implementations (required for pure tdoa processing) */
        geoMission.setFilterProcessNoise(eye.scalarMultiply(0.01).getData()); //setFilterProcessNoise(new double[][]{{0.01, 0, 0, 0}, {0, 0.01 ,0, 0}, {0, 0, 0.01, 0}, {0, 0, 0 ,0.01}});
        log.debug("Set filter process noise data as: "+eye.scalarMultiply(0.01));

        Xk = Xinit;

        Pk = Pinit.scalarMultiply(1000.0);

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
        log.info("Running for # observations:"+observations.size());
        if (observations.size()==0) {
            log.info("No observations returning");
            return null;
        }

        running.set(true);

        dispatchResult(Xk);

        Vector<FilterObservationDTO> filterObservationDTOs = new Vector<FilterObservationDTO>();

        FilterStateDTO filterStateDTO = new FilterStateDTO();

        long startTime = Calendar.getInstance().getTimeInMillis();

        if (this.geoMission.getOutputFilterState()) {
            kmlFileHelpers = new KmlFileHelpers();
            kmlFileHelpers.provisionFilterStateExport();
            log.debug("Provisioned filter state export");
        }
        int filterStateExportCounter = 0;

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

            /* reinitialise various collections */
            innov = new ArrayRealVector(matrice_size); //(innovd);
            P_innov = new Array2DRowRealMatrix(matrice_size, matrice_size); //(P_innovd);
            filterObservationDTOs.removeAllElements();
            RealVector nonAoaNextState = null;
            Iterator obsIterator = this.observations.values().iterator();

            log.trace("Innovating with # Observations: "+this.observations.size());

            while (obsIterator.hasNext()) {

                Observation obs = (Observation) obsIterator.next();
                // SAFETY Check
                //log.debug("Obs ["+obs.getTargetId()+"] relates to a tracked target?: "+this.geoMission.getTargets().keySet().contains(obs.getTargetId()));
                //if (this.geoMission.getTargets().keySet().contains(obs.getTargetId())) {

                    // For each observation, get the state for the target its attempting to measure.
                    // I.e. the Target if is a single target run, will be the first two variables.
                    //    If two target run, will be either the first or second params. The observation should know what target it is.
                    //     Needs to support many targets, and ability to retrieve that targets state easily
                    //     A map containing the filter state x/y indexes, mapped to the targets ID?
                    // State index map should be set up upon mission creation
                    Integer[] stateIndexes = stateIndexMap.get(obs.getTargetId());
                    log.trace("Retrieved state indices: " + stateIndexes[0] + "," + stateIndexes[1]);

                    xk = Xk.getEntry(stateIndexes[0]);
                    yk = Xk.getEntry(stateIndexes[1]);


                    double f_est = 0.0;
                    double d = 0.0;
                    H = null;

                    if (obs.getObservationType().equals(ObservationType.range)) {

//                    H = recalculateH(obs.getX(), obs.getY(), xk, yk);
                        double R1 = Math.sqrt(Math.pow((obs.getX() - xk), 2) + Math.pow(obs.getY() - yk, 2));    // Note: obs.X/Y are the tower (asset) known locations
                        double dfdx = -(obs.getX() - xk) / R1;
                        double dfdy = -(obs.getY() - yk) / R1;

                        /// TODO, need to pad out the jacobian, to be dynamic against state size. Since this observation only relates to state of xk,yk df/dxy are zero??
                        H = new Array2DRowRealMatrix(1, matrice_size);

                        // Set Jacobian based on which target it is, lookup stateIndex map

                        H.setEntry(0, stateIndexes[0] + (matrice_size / 2), dfdx); // create like this: double[][], {{0, 0, dfdx, dfdy}}; [single target example]
                        H.setEntry(0, stateIndexes[1] + (matrice_size / 2), dfdy);
                        //log.debug("Created H as: "+H);
                        // H will determine how much to change the relavant state. Since the obs relates to a single target, it should only need to have representation against it and not the other targets?
                        // Except for TDOA? There is an interdependency???

//                    double[][] jacobianData = {{0, 0, dfdx, dfdy}};  // DEPRECATED IN NAV
//                    H = new Array2DRowRealMatrix(jacobianData);

                        f_est = Math.sqrt(Math.pow((obs.getX() - xk), 2) + Math.pow(obs.getY() - yk, 2));

                        d = obs.getMeas();

                        log.trace("RANGE innovation: " + f_est + ", vs d: " + d);
                    } else if (obs.getObservationType().equals(ObservationType.tdoa)) {


                        // If its TDOA, also need state indexes for the second asset the observation is using
                        Integer[] stateIndexes_b = stateIndexMap.get(obs.getTargetId_b());
                        double xk_b = Xk.getEntry(stateIndexes_b[0]);
                        double yk_b = Xk.getEntry(stateIndexes_b[1]);


                        //H = recalculateH_TDOA(obs.getX(), obs.getY(), obs.getXb(), obs.getYb(), xk, yk);//.scalarMultiply(-1); // temp scalar mult this oddly seems to fix an issue
                        // ref method: public RealMatrix recalculateH_TDOA(double x, double y, double x2, double y2, double Xk1, double Xk2) {
                        double R1 = Math.sqrt(Math.pow((obs.getX() - xk), 2) + Math.pow(obs.getY() - yk, 2));
                        double R2 = Math.sqrt(Math.pow((obs.getX() - xk_b), 2) + Math.pow(obs.getY() - yk_b, 2));

                        double dfdx = (-obs.getX() + xk) / R1 - (-obs.getX() + xk_b) / R2;
                        double dfdy = (-obs.getY() + yk) / R1 - (-obs.getY() + yk_b) / R2;

                        H = new Array2DRowRealMatrix(1, matrice_size);
                        // Set Jacobian based on which target it is, lookup stateIndex map
                        // NOTE: for TDOA, since dependent secondary asset is a state also, need to share the state innovations, split them
                        H.setEntry(0, stateIndexes[0] + (matrice_size / 2), dfdx/2); // create like this: double[][], {{0, 0, dfdx, dfdy}}; [single target example]
                        H.setEntry(0, stateIndexes[1] + (matrice_size / 2), dfdy/2);
                        H.setEntry(0, stateIndexes_b[0] + (matrice_size / 2), dfdx/2); // create like this: double[][], {{0, 0, dfdx, dfdy}}; [single target example]
                        H.setEntry(0, stateIndexes_b[1] + (matrice_size / 2), dfdy/2);
                        //log.debug("Created H as: "+H);
                        // H will determine how much to change the relavant state. Since the obs relates to a single target, it should only need to have representation against it and not the other targets?
                        // Except for TDOA? There is an interdependency???
                        // TODO, perhaps need to produce df/dx1,df/dy1,df/dx2,df/dy2 ???

//                    double[][] jacobianData = {{0, 0, dfdx, dfdy}}; --- DEPRECATED IN NAV
//                    H = new Array2DRowRealMatrix(jacobianData);


                        // Swapped the meaning of Obs::X/Y - now means the true loc of the transmitter.
                        //     xk/yk,xk_b/yk_b are now the estimated locations of targets (i.e. own position)
                        f_est = Math.sqrt(Math.pow((xk - obs.getX()), 2) + Math.pow(yk - obs.getY(), 2)) - Math.sqrt(Math.pow((xk_b - obs.getX()), 2) + Math.pow(yk_b - obs.getY(), 2));

                        d = obs.getMeas() * Helpers.SPEED_OF_LIGHT;

                        log.trace("TDOA innovation: " + f_est + ", vs d: " + d);
                    } else if (obs.getObservationType().equals(ObservationType.aoa)) {

                        //H = recalculateH_AOA(obs.getX(), obs.getY(), xk, yk);
                        // ref method: public RealMatrix recalculateH_AOA(double x, double y, double Xk1, double Xk2)
                        double R1 = Math.sqrt(Math.pow((obs.getX() - xk), 2) + Math.pow((obs.getY() - yk), 2)); // Note: better performance using sqrt
                        double dfdx = (obs.getY() - yk) / R1;  // Note d/d"x" = "y - y_est"/..... on purpose linearisation   /// ORIGINAL
                        double dfdy = -(obs.getX() - xk) / R1;
//                        double dfdx = ( yk - obs.getY()) / R1;  // Note d/d"x" = "y - y_est"/..... on purpose linearisation
//                        double dfdy = -(xk - obs.getX()) / R1;

                        H = new Array2DRowRealMatrix(1, matrice_size);
                        H.setEntry(0, stateIndexes[0] + (matrice_size / 2), dfdx); // create like this: double[][], {{0, 0, dfdx, dfdy}}; [single target example], {{0,0,0,0,df/dx1,df/dy1,df/dx2,df/y2}} [duel tgt example]
                        H.setEntry(0, stateIndexes[1] + (matrice_size / 2), dfdy);
                        //log.debug("Created H as: "+H);
//                    double[][] jacobianData = {{0, 0, dfdx, dfdy}}; --- DEPRECATED IN NAV
//                    H = new Array2DRowRealMatrix(jacobianData);

                        f_est = Math.atan(( obs.getY() - yk) / ( obs.getX() - xk)) * 180 / Math.PI; /// ORIGINAL
                        //f_est = Math.atan(( yk - obs.getY()) / ( xk - obs.getX())) * 180 / Math.PI;

                        if (xk < obs.getX()) { /// ORIGINAL
                            f_est = f_est + 180;
                        }

                        if (yk < obs.getY() && xk >= obs.getX()) {
                            f_est = 360 - Math.abs(f_est);
                        }
//                        if (obs.getX() < xk) {
//                            f_est = f_est + 180;
//                        }
//
//                        if (obs.getY() < yk && obs.getX() >= xk) {
//                            f_est = 360 - Math.abs(f_est);
//                        }


                        /// TEMP DEV TESTING
                        //f_est = 2*f_est;


                        d = obs.getMeas() * 180 / Math.PI;

                        //log.debug("AOA innovation: " + f_est + ", vs d: " + d);
                    }

                    RealMatrix toInvert = (H.multiply(Pk).multiply(H.transpose()).add(Rk));
                    RealMatrix Inverse = (new LUDecomposition(toInvert)).getSolver().getInverse();

                    K = Pk.multiply(H.transpose()).multiply(Inverse).scalarMultiply(this.geoMission.getFilterRangeBias());

                    double rk = d - f_est;

                /* '360-0 Conundrum' adjustment */
                    if (obs.getObservationType().equals(ObservationType.aoa)) {
                        if (innov.getEntry(0) != 0.0) {
                            if (nonAoaNextState == null) {
                                nonAoaNextState = Xk.add(innov);
                            }

                            // TODO, this needs to be stateIndex aware. It is creating an effect where innovations are created for the wrong state/target

                        /* gradient from obs to prevailing pressure direction */
                                // Math.atan((nonAoaNextState.getEntry(stateIndexes[0] + (matrice_size / 2)) - obs.getY()) / (nonAoaNextState.getEntry(stateIndexes[1]) - obs.getX())) * 180 / Math.PI;
                                //     stateIndexes[1] + matrice_size/2
                            //double pressure_angle = Math.atan((nonAoaNextState.getEntry(2) - obs.getY()) / (nonAoaNextState.getEntry(0) - obs.getX())) * 180 / Math.PI;
                            double pressure_angle = Math.atan((nonAoaNextState.getEntry(stateIndexes[0] + (matrice_size / 2)) - obs.getY()) / (nonAoaNextState.getEntry(stateIndexes[1]) - obs.getX())) * 180 / Math.PI;
                            /// /log.debug("P-ang: "+pressure_angle+", f_est: "+f_est+", Yp: "+innov.getEntry(3)+", Pressure: "+nonAoaNextState+", INNOV: "+innov);
                            if (nonAoaNextState.getEntry(stateIndexes[0]) < obs.getX()) {
                                pressure_angle = pressure_angle + 180;
                            }

                            if (nonAoaNextState.getEntry(stateIndexes[1]) < obs.getY() && nonAoaNextState.getEntry(stateIndexes[0]) >= obs.getX()) {
                                pressure_angle = 360 - Math.abs(pressure_angle);
                            }
                            //log.debug("P-ang (adjusted): "+pressure_angle);


                            if (Math.abs(pressure_angle) > Math.abs(f_est)) {
                                if (Xk.getEntry(stateIndexes[1]) > obs.getY() && Xk.getEntry(stateIndexes[0]) > obs.getX()) {
                                    rk = Math.abs(rk);
                                } else if (Xk.getEntry(stateIndexes[1]) > obs.getY() && Xk.getEntry(stateIndexes[0]) < obs.getX()) {
                                    rk = -Math.abs(rk);
                                } else if (Xk.getEntry(stateIndexes[1]) < obs.getY() && Xk.getEntry(stateIndexes[0]) < obs.getX()) {
                                    rk = Math.abs(rk);
                                } else if (Xk.getEntry(stateIndexes[1]) < obs.getY() && Xk.getEntry(stateIndexes[0]) > obs.getX()) {
                                    rk = -Math.abs(rk);
                                }
                            } else {
                                if (Xk.getEntry(stateIndexes[1]) > obs.getY() && Xk.getEntry(stateIndexes[0]) > obs.getX()) {
                                    rk = -Math.abs(rk);
                                } else if (Xk.getEntry(stateIndexes[1]) > obs.getY() && Xk.getEntry(stateIndexes[0]) < obs.getX()) {
                                    rk = Math.abs(rk);
                                } else if (Xk.getEntry(stateIndexes[1]) < obs.getY() && Xk.getEntry(stateIndexes[0]) < obs.getX()) {
                                    rk = -Math.abs(rk);
                                } else if (Xk.getEntry(stateIndexes[1]) < obs.getY() && Xk.getEntry(stateIndexes[0]) > obs.getX()) {
                                    rk = Math.abs(rk);
                                }
                            }
                        }
                    }

                    double[] HXk = H.operate(Xk).toArray();
                    RealVector innov_ = K.scalarMultiply(rk - HXk[0]).getColumnVector(0);

                    innov = innov_.add(innov);

                    P_innov = K.multiply(H).multiply(Pk).add(P_innov);

                    log.trace("Adding observation for tracking: "+obs.toString());
                    filterObservationDTOs.add(new FilterObservationDTO(obs, f_est, innov_));
                //}

                Xk = Xk.add(innov);
                Pk = (eye.multiply(Pk)).subtract(P_innov);

            /* Export filter state - development debugging */
                if (this.geoMission.getOutputFilterState()) {
                    filterStateExportCounter++;
                    if (filterStateExportCounter == 10) {
                        // Only if it is changing significantly
                        double residual = Math.abs(innov.getEntry(2)) + Math.abs(innov.getEntry(3));
                        if (residual > 0.5) {
                            filterStateDTO.setFilterObservationDTOs(filterObservationDTOs);
                            filterStateDTO.setXk(Xk);
                            kmlFileHelpers.exportAdditionalFilterState(this.geoMission, filterStateDTO, residual);
                            filterStateExportCounter = 0;
                        }
                    }

                    // TEMP
                    //dispatchResult(Xk);
                }


            }


            ////  MOVED HERE

            /* Export Result */
            if ((Calendar.getInstance().getTimeInMillis() - startTime) > this.geoMission.getDispatchResultsPeriod()) {


                /* Check convergence of all targets */
                boolean allTargetsConverged = true;
                for (Target target : this.geoMission.getTargets().values()) {

                    Integer[] stateIndexes = stateIndexMap.get(target.getId());

                    /* A measure of residual changes the filter intends to make - using delta_x/y innovation data */
                    double residual = Math.abs(innov.getEntry(stateIndexes[0] + (matrice_size / 2))) + Math.abs(innov.getEntry(stateIndexes[1] + (matrice_size / 2)));
                    log.trace("Residual, for target: [" + target.getId() + "]: " + residual);


                    /// NOTE: if residual threshold not met here, will be stuck in loop


                    // ADDED in _nav: Normalise the residual measurement
                    residual = residual / this.observations.size();


                    if (residual < this.geoMission.getFilterDispatchResidualThreshold()) {

//                        log.debug("Innov: "+ innov);
//                        log.debug("Delta-x innovation: "+ innov.getEntry(stateIndexes[0] + (matrice_size / 2)));
//                        log.debug("Delta-y innovation: "+ innov.getEntry(stateIndexes[1] + (matrice_size / 2)));
//                        log.debug("Delta-x innovation Abs: "+ Math.abs(innov.getEntry(stateIndexes[0] + (matrice_size / 2))));
//                        log.debug("Delta-y innovation Abs: "+ Math.abs(innov.getEntry(stateIndexes[1] + (matrice_size / 2))));
//                        log.debug("Residual: "+ (Math.abs(innov.getEntry(stateIndexes[0] + (matrice_size / 2))) + Math.abs(innov.getEntry(stateIndexes[1] + (matrice_size / 2)))));

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

                        //dispatchResult(Xk);
                        GeolocationResult geolocationResult = summariseResult(Xk);
                        ComputeResults computeResults = new ComputeResults();
                        computeResults.setGeolocationResult(geolocationResult);
                        computeResults.setGeoId(this.geoMission.getGeoId());

                        /* Dispatch Result to listeners */
                        dispatchResult(computeResults);


                        // TODO, if all targets are below threshold then break

                        if (geoMission.getMissionMode().equals(MissionMode.fix)) {

                            /// DEVING
                            log.debug("Residual: "+residual+", vs filterMeasurementError: "+this.geoMission.getFilterMeasurementError());
                            if (residual < this.geoMission.getFilterMeasurementError()/10) {

                                if (residual < this.geoMission.getFilterConvergenceResidualThreshold()) {  /// ORIGINAL

                                    log.debug("Exiting since this is a FIX Mode run and filter has converged to threshold");
                                    running.set(false);
                                    break;

                                } else {
                                    allTargetsConverged = false;
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
                        allTargetsConverged = false;
                    }
                }
                if (allTargetsConverged && geoMission.getMissionMode().equals(MissionMode.fix)) {
                    log.debug("Exiting since this is a FIX Mode run and all targets in the filter have converged to threshold");
                    running.set(false);
                    break;
                }
            }

        }

        GeolocationResult geolocationResult = summariseResult(Xk);

        ComputeResults computeResults = new ComputeResults();
        computeResults.setGeolocationResult(geolocationResult);
        computeResults.setGeoId(this.geoMission.getGeoId());
        /* Dispatch Result to listeners */
        dispatchResult(computeResults);

        return computeResults;
    }

    public RealMatrix recalculateH(double x_rssi, double y_rssi, double Xk1, double Xk2) {

        double R1 = Math.sqrt(Math.pow((x_rssi-Xk1),2) + Math.pow(y_rssi-Xk2,2));

        double dfdx = -(x_rssi-Xk1)/R1;
        double dfdy = -(y_rssi-Xk2)/R1;

        double[][] jacobianData = {{0, 0, dfdx, dfdy}};
        RealMatrix H = new Array2DRowRealMatrix(jacobianData);
        return H;
    }

    public RealMatrix recalculateH_TDOA(double x, double y, double x2, double y2, double Xk1, double Xk2) {

        double R1 = Math.sqrt(Math.pow((x-Xk1),2) + Math.pow(y-Xk2,2));
        double R2 = Math.sqrt(Math.pow((x2-Xk1),2) + Math.pow(y2-Xk2,2));

        double dfdx = (-x+Xk1)/R1 - (-x2+Xk1)/R2;
        double dfdy = (-y+Xk2)/R1 - (-y2+Xk2)/R2;

        double[][] jacobianData = {{0, 0, dfdx, dfdy}};
        RealMatrix H = new Array2DRowRealMatrix(jacobianData);
        return H;
    }

    public RealMatrix recalculateH_AOA(double x, double y, double Xk1, double Xk2) {

        double R1 = Math.sqrt(Math.pow((x-Xk1),2) + Math.pow((y-Xk2),2)); // Note: better performance using sqrt

        double dfdx = (y-Xk2)/R1;  // Note d/d"x" = "y - y_est"/..... on purpose linearisation
        double dfdy = -(x-Xk1)/R1;

        double[][] jacobianData = {{0, 0, dfdx, dfdy}};
        RealMatrix H = new Array2DRowRealMatrix(jacobianData);
        return H;
    }

    public double[] findRudimentaryStartPoint(Asset asset_a, Asset asset_b, double addition) {
        double x_init=0; double y_init=0;
        double[] asset_a_utm = Helpers.convertLatLngToUtmNthingEasting(asset_a.getCurrent_loc()[0],asset_a.getCurrent_loc()[1]);
        double[] asset_b_utm = Helpers.convertLatLngToUtmNthingEasting(asset_b.getCurrent_loc()[0],asset_b.getCurrent_loc()[1]);
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

    public void dispatchResult(RealVector Xk) {

        //log.debug("State: "+Xk.getEntry(0)+","+Xk.getEntry(1));
        log.debug("Dispatching result for # targets: "+this.geoMission.getTargets().size());

        for (Target target : this.geoMission.getTargets().values()) {
            log.debug("Dispatching result for target: "+target.getId());

            // Get StateIndexes for each target
            Integer[] indexes = stateIndexMap.get(target.getId());

            double[] latLon = Helpers.convertUtmNthingEastingToLatLng(Xk.getEntry(indexes[0]), Xk.getEntry(indexes[1]), this.geoMission.getLatZone(), this.geoMission.getLonZone());

            log.debug("Geomission targets size: "+this.geoMission.getTargets().size());
            this.geoMission.getTargets().get(target.getId()).setCurrent_loc(latLon);

            /* Compute probability ELP */
            double[][] covMatrix = new double[][]{{Pk.getEntry(0, 0), Pk.getEntry(0, 1)}, {Pk.getEntry(1, 0), Pk.getEntry(1, 1)}};
            log.debug("Pk: " + Pk);
            double[] evalues = Helpers.getEigenvalues(covMatrix);
            double largestEvalue = Math.max(evalues[0], evalues[1]);
            double smallestEvalue = Math.min(evalues[0], evalues[1]);
            log.debug("Large e-value: " + largestEvalue + ", Smallest e-value: " + smallestEvalue);
            double[] evector = Helpers.getEigenvector(covMatrix, largestEvalue);
            double rot = Math.atan(evector[1] / evector[0]);
            double major = 2 * Math.sqrt(9.210 * largestEvalue); // 5.991 equiv 95% C.I, 4.605 equiv 90% C.I, 9.210 equiv 99% C.I
            double minor = 2 * Math.sqrt(9.210 * smallestEvalue);
            this.geoMission.getTargets().get(target.getId()).setElp_major(major);
            this.geoMission.getTargets().get(target.getId()).setElp_minor(minor);
            this.geoMission.getTargets().get(target.getId()).setElp_rot(rot);

            this.efusionListener.result(geoMission.getGeoId(),target.getId(),latLon[0],latLon[1], major, minor, rot);

            // TODO, load up a state buffer in the process manager????
            //    THIS doesn't work, different GM object
            //this.geoMission.getResultBuffer().put(target.getId(), new GeoResult(geoMission.getGeoId(),target.getId(),latLon[0],latLon[1], major, minor, rot));
            this.internalListener.result(geoMission.getGeoId(),target.getId(),latLon[0],latLon[1], major, minor, rot);
        }


        if (this.geoMission.getOutputFilterState() && kmlFileHelpers !=null) {
            kmlFileHelpers.writeCurrentExports(this.geoMission);
        }

        if (this.geoMission.getOutputKml()) {
            KmlFileStaticHelpers.exportGeoMissionToKml(this.geoMission);
        }
    }

    public void reinitialiseFilter() {
        /* Select two assets by random and use their middle point */
        Random rand = new Random();
        List<Asset> assetList = new ArrayList<Asset>(this.geoMission.getAssets().values());
        Asset randAssetA = assetList.get(rand.nextInt(assetList.size()));
        assetList.remove(randAssetA);
        Asset randAssetB = assetList.get(rand.nextInt(assetList.size()));
        log.debug("Finding rudimentary start point between two random observations: "+randAssetA.getId()+","+randAssetB.getId());

        double[] start_x_y = findRudimentaryStartPoint(randAssetA, randAssetB, -500);
        log.debug("(Re) Filter start point: "+start_x_y[0]+","+start_x_y[1]);
        double[] initStateData = {start_x_y[0], start_x_y[1], 1, 1};
        log.info("(Re) Init State Data Easting/Northing: "+initStateData[0]+","+initStateData[1]+",1,1");
        double[] latLonStart = Helpers.convertUtmNthingEastingToLatLng(initStateData[0], initStateData[1], geoMission.getLatZone(), geoMission.getLonZone());
        log.info("(Re) Init start point: "+latLonStart[0]+","+latLonStart[1]);
        RealVector Xinit = new ArrayRealVector(initStateData);
        Xk = Xinit;
    }

    public void resetCovariances(){
        log.debug("Resetting covariances, from: "+Pk);
        Pk = Pinit.scalarMultiply(1000.0);
        log.debug("Resetting covariances, to: "+Pk);
    }

    public synchronized void setStaged_observations(Map<Long, Observation> staged_observations) {
        this.staged_observations = staged_observations;
    }

    public GeolocationResult summariseResult(RealVector Xk) {

        double[] latLon = Helpers.convertUtmNthingEastingToLatLng(Xk.getEntry(0),Xk.getEntry(1), this.geoMission.getLatZone(), this.geoMission.getLonZone());

        /* Compute probability ELP */
        double[][] covMatrix=new double[][]{{Pk.getEntry(0,0),Pk.getEntry(0,1)},{Pk.getEntry(1,0),Pk.getEntry(1,1)}};
        double[] evalues = Helpers.getEigenvalues(covMatrix);
        double largestEvalue = Math.max(evalues[0],evalues[1]);
        double smallestEvalue = Math.min(evalues[0],evalues[1]);
        double[] evector = Helpers.getEigenvector(covMatrix, largestEvalue);
        double rot = Math.atan(evector[1] / evector[0]);
        /* This angle is between -pi -> pi, adjust 0->2pi */
        if (rot<0)
            rot = rot + 2*Math.PI;
        /* Ch-square distribution for two degrees freedom: 1.39 equiv 50% (i.e. CEP), 5.991 equiv 95% C.I, 4.605 equiv 90% C.I, 9.210 equiv 99% C.I */
        double half_major_axis_length = Math.sqrt(largestEvalue)*1.39; // Orig used: 2*Math.sqrt(9.210*largestEvalue);
        double half_minor_axis_length = Math.sqrt(smallestEvalue)*1.39;

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
        //geolocationResult.setResidual_rk(residual_rk);

        return geolocationResult;
    }

    public void dispatchResult(ComputeResults computeResults) {

        this.geoMission.getTarget().setCurrent_loc(new double[]{computeResults.getGeolocationResult().getLat(),computeResults.getGeolocationResult().getLon()});
        //this.geoMission.setComputeResults(computeResults);
        // OLD WAYthis.efusionListener.result(geoMission.getGeoId(),latLon[0],latLon[1], this.geoMission.getTarget().getElp_major(), this.geoMission.getTarget().getElp_minor(), rot);
        this.efusionListener.result(computeResults);

        if (this.geoMission.getOutputFilterState() && kmlFileHelpers !=null) {
            /* Write non-static file exports, includes filter state data stored in mem */
            kmlFileHelpers.writeCurrentExports(this.geoMission);
        }

        if (this.geoMission.getOutputKml()) {
            KmlFileStaticHelpers.exportGeoMissionToKml(this.geoMission);
        }
    }

}
