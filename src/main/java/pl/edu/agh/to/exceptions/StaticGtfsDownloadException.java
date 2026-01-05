package pl.edu.agh.to.exceptions;

public class StaticGtfsDownloadException extends StaticGtfsException {

    public StaticGtfsDownloadException(String message) {
        super(message);
    }

    public StaticGtfsDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}