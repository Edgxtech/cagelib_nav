package tech.tgo.efusion.compute;

import org.apache.commons.math3.linear.RealVector;

import java.util.List;

/**
 * @author Timothy Edge (timmyedge)
 */
public class FilterStateDTO {
    List<FilterObservationDTO> filterObservationDTOs;
    RealVector Xk;

    public List<FilterObservationDTO> getFilterObservationDTOs() {
        return filterObservationDTOs;
    }

    public void setFilterObservationDTOs(List<FilterObservationDTO> filterObservationDTOs) {
        this.filterObservationDTOs = filterObservationDTOs;
    }

    public RealVector getXk() {
        return Xk;
    }

    public void setXk(RealVector xk) {
        Xk = xk;
    }
}
