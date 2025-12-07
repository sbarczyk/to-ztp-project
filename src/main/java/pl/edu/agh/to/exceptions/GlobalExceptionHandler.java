package pl.edu.agh.to.exceptions;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles exceptions related to critical business logic failures (e.g., no data available).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        log.error("Logic error occurred: {}", ex.getMessage());
        return new ResponseEntity<>("Internal Server Error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles exceptions related to Protobuf data parsing errors.
     */
    @ExceptionHandler(InvalidProtocolBufferException.class)
    public ResponseEntity<String> handleParsingException(InvalidProtocolBufferException ex) {
        log.error("Parsing error occurred: {}", ex.getMessage());
        return new ResponseEntity<>("Internal Server Error: Data parsing failed.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}