package tech.tgo.efusion.util;

import tech.tgo.efusion.model.Target;

import java.util.List;

/**
 * @author Timothy Edge (timmyedge)
 */
public class TestTarget extends Target {
    Boolean provide_range;
    Boolean provide_aoa;
    Boolean provide_tdoa;
    List<String> tdoa_target_ids;

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

    public List<String> getTdoa_target_ids() {
        return tdoa_target_ids;
    }

    public void setTdoa_target_ids(List<String> tdoa_target_ids) {
        this.tdoa_target_ids = tdoa_target_ids;
    }
}
