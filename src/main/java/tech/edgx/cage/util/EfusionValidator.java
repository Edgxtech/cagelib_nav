package tech.edgx.cage.util;

import tech.edgx.cage.model.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.edgx.cage.model.GeoMission;
import tech.edgx.cage.model.Observation;
import tech.edgx.cage.model.ObservationType;

import java.util.Collection;
import java.util.Set;

public class EfusionValidator {

    private static final Logger log = LoggerFactory.getLogger(EfusionValidator.class);

    public static void validate(GeoMission geoMission) throws Exception {
        if (geoMission.getOutputKml()!=null && geoMission.getOutputKml().equals(true) && geoMission.getOutputKmlFilename()==null) {
            throw new ConfigurationException("Output KML is selected however no output filname was specified");
        }
        if (geoMission.getMissionMode()==null) {
            throw new ConfigurationException("Mode was not specified: fix or track");
        }
        if (geoMission.getGeoId()==null) {
            throw new ConfigurationException("Mission id was not set: specify a unique string label");
        }
        if (geoMission.getFilterUseSpecificInitialCondition()==null) {
            // Use default: false, i.e. use random initial condition
            log.debug("'Filter use specific initial condition' property not set, using default: false i.e. select at random");
            geoMission.setFilterUseSpecificInitialCondition(false);
        }
        else if (geoMission.getFilterUseSpecificInitialCondition()!=null && geoMission.getFilterUseSpecificInitialCondition()) {
            if (geoMission.getFilterSpecificInitialLat()==null) {
                throw new ConfigurationException("Use of specific init condition was desired, however specific LATITUDE was not provided");
            }
            if (geoMission.getFilterSpecificInitialLon()==null) {
                throw new ConfigurationException("Use of specific init condition was desired, however specific LONGITUDE was not provided");
            }
        }
    }

    public static void validate(Observation observation, Set<String> targets) throws Exception {
        log.debug("Validating obs for tgt: "+observation.getTargetId());
        if (!targets.contains(observation.getTargetId())) {
            throw new ObservationException("Observation is for a target ["+ observation.getTargetId()+"] not tracked by this mission process, reconfigure mission first");
        }
    }

    public static void validate(Observation observation) throws Exception {
        double lat = Math.abs(observation.getLat()); double lon = Math.abs(observation.getLon());
        if (lat < 0 || lat>90) {
            throw new ObservationException("Assets latitude should range between +- 0->90");
        }
        if (lon < 0 || lon>180) {
            throw new ObservationException("Assets longitude should range between +- 0->180");
        }
        if (observation.getAssetId()==null || observation.getAssetId().isEmpty()) {
            throw new ObservationException("No Asset Id was specified");
        }
        if (observation.getObservationType()==null) {
            throw new ObservationException("No observation type was specified");
        }
        if (observation.getObservationType().equals(ObservationType.range)) {
            if (observation.getMeas()==0.0) {
                throw new ObservationException("No observation range value was specified for observation type "+observation.getObservationType().name());
            }
            if (observation.getMeas()<0) {
                throw new ObservationException("Range value should be positive for observation type "+observation.getObservationType().name());
            }
        }
        else if (observation.getObservationType().equals(ObservationType.tdoa)) {
            log.warn("Ignoring TDOA meas, not implemented");
            throw new ObservationException("Ignoring TDOA meas, not implemented");
        }
        else if (observation.getObservationType().equals(ObservationType.aoa)) {
            if (observation.getMeas()==0.0) {
                throw new ObservationException("No observation aoa value was specified for observation type "+observation.getObservationType().name());
            }
            if (observation.getMeas()<0 || observation.getMeas()>2*Math.PI) {
                throw new ObservationException("AOA value should be between 0 and 2*pi for observation type "+observation.getObservationType().name());
            }
        }
    }

    public static void validateTargets(Collection<Target> targets) throws Exception {
        for (Target target : targets) {
            validateTarget(target);
        }
    }

    public static void validateTarget(Target target) throws Exception {
        log.debug("Validating tgt: " + target.getId());
        if (target.getId()==null) {
            throw new ConfigurationException("Target description has no Id");
        }
        if (target.getName()==null) {
            throw new ConfigurationException("Target description for target [" + target.getId() + "] has no name");
        }
    }
}
