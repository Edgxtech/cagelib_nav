package au.com.ausstaker.cage.util;

/**
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public class ObservationException extends Exception {

    public String message;

    public ObservationException(String message) {
        this.message = message;
    }
    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
