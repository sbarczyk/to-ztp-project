package pl.edu.agh.to.exceptions;

public class StaticGtfsExtractException extends StaticGtfsException {

    public StaticGtfsExtractException(String message) {
        super(message);
    }

    public StaticGtfsExtractException(String message, Throwable cause) {
        super(message, cause);
    }
}