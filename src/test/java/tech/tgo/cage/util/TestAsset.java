package tech.tgo.cage.util;

import tech.tgo.cage.model.Asset;

/**
 * @author Timothy Edge (timmyedge)
 */
public class TestAsset extends Asset {
    Boolean provide_range;
    Boolean provide_aoa;
    Boolean provide_tdoa;
//    List<String> tdoa_asset_ids; // MOVED TO TEST TARGET IN NAV

    public Boolean getProvide_range() {
        return provide_range;
    }

    public void setProvide_range(Boolean provide_range) {
        this.provide_range = provide_range;
    }

    public Boolean getProvide_aoa() {
        return provide_aoa;
    }

    public void setProvide_aoa(Boolean provide_aoa) {
        this.provide_aoa = provide_aoa;
    }

    public Boolean getProvide_tdoa() {
        return provide_tdoa;
    }

    public void setProvide_tdoa(Boolean provide_tdoa) {
        this.provide_tdoa = provide_tdoa;
    }

//    public List<String> getTdoa_asset_ids() {
//        return tdoa_asset_ids;
//    }
//
//    public void setTdoa_asset_ids(List<String> tdoa_asset_ids) {
//        this.tdoa_asset_ids = tdoa_asset_ids;
//    }
}