package au.com.ausstaker.cage.util;

import au.com.ausstaker.cage.model.Asset;

/**
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public class TestAsset extends Asset {
    Boolean provide_range;
    Boolean provide_aoa;
    Boolean provide_tdoa;

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
}
