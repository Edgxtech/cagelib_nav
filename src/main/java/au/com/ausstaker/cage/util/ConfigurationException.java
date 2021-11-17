package au.com.ausstaker.cage.util;

/**
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public class ConfigurationException extends Exception {

    public String message;

    public ConfigurationException(String message) {
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
