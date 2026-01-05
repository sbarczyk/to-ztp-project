package pl.edu.agh.to.exceptions;

public class StaticGtfsException extends RuntimeException {

    public StaticGtfsException(String message) {
        super(message);
    }

    public StaticGtfsException(String message, Throwable cause) {
        super(message, cause);
    }
}