# CaGE Library - Navigation (cagelib_nav)

Compound Geolocation Engine (CaGE) is a sensor fusion library which utilises an Extended Kalman Filter for position location and tracking

## Features

    * Own location estimation with Tracking and Fix Mode
    * Takes sensor measurements including AOA and Range
    * Optional generation of KML result output, client implementation alternate

## How to Use

    /* Add to project dependencies */
    <dependency>
        <groupId>tech.edgx.cage</groupId>
        <artifactId>cagelib_nav</artifactId>
        <version>0.1</version>
    </dependency>

    /* Implement eFusion listener */
    public class ClientCustomController implements EfusionListener {

        /* Client side needs to manage geomission references for callback response */
        Map<String,GeoMission> missionsMap = new HashMap<String,GeoMission>();

        ...

        /* Result callback */
        @Override
        public void result(String geoId, double lat, double lon, double cep_elp_maj, double cep_elp_min, double cep_elp_rot) {
            log.debug("Result -> GeoId: " + geoId + ", Lat: " + lat + ", Lon: " + lon + ", CEP major: " + cep_elp_maj + ", CEP minor: " + cep_elp_min + ", CEP rotation: " + cep_elp_rot);
        }
    }

    /* Configure the intended mission - See GeoMission class for other optional parameters */
    GeoMission geoMission = new GeoMission();
    geoMission.setMissionMode(MissionMode.track); /* Alternative MissionMode.fix */
    geoMission.setTarget(new Target("MY_TGT_ID","MY_TGT_NAME"));
    geoMission.setGeoId("MY_GEO_ID");
    geoMission.setShowMeas(true);
    geoMission.setShowCEPs(true);
    geoMission.setShowGEOs(true);
    geoMission.setOutputKml(true);
    geoMission.setOutputKmlFilename("geoOutput.kml");
    geoMission.setShowTrueLoc(true);

    try {
        efusionProcessManager.configure(geoMission);
    }
    catch (Exception e) {}

    /* Client side needs to manage geomission references for callback response */
    missionsMap.put(geoMission.getGeoId(), geoMission);

    /* Add Observation - AOA Type Example */
    Observation observation = new Observation(obsId, asset.getId(), asset.getCurrent_loc()[0], asset.getCurrent_loc()[1]);
    observation.setMeas(meas_aoa); // aoa in radians
    observation.setObservationType(ObservationType.aoa);

    /* Add Observation - Range Type Example */
    Observation observation = new Observation(obsId, asset.getId(), asset.getCurrent_loc()[0], asset.getCurrent_loc()[1]);
    observation.setMeas(meas_range); // range in metres
    observation.setObservationType(ObservationType.range);

    /* Add Observation - TDOA Type Example */
    Observation observation = new Observation(obsId, asset.getId(), asset.getCurrent_loc()[0], asset.getCurrent_loc()[1]);
    observation.setAssetId_b(testAssets.get(secondary_asset_id).getId());
    observation.setLat_b(asset1.getCurrent_loc()[0]);
    observation.setLon_b(asset1.getCurrent_loc()[1]);
    observation.setMeas(meas_tdoa); // tdoa in seconds
    observation.setObservationType(ObservationType.tdoa);

    /* Start Computing Thread - Tracking Mode */
    try {
        efusionProcessManager.start();
        Thread.sleep(40000);
        timer.cancel();
    }
    catch (Exception e) {}

    /* Start Computing Thread - Fix Mode */
    try {
        Thread thread = efusionProcessManager.start();
        thread.join();
    }
    catch (Exception e) {}

## Copyright / License

    Copyright (c) Edgx Technology

    This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at http://www.gnu.org/licenses/lgpl.html.