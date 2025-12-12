package pl.edu.agh.to.exceptions;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles exceptions related to business logic and missing data
     * (e.g. no trips, no stops, no departure time).
     */
    @ExceptionHandler({
            IllegalStateException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<String> handleLogicExceptions(RuntimeException ex) {
        log.error("Logic error occurred: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error: " + ex.getMessage());
    }

    /**
     * Handles exceptions related to Protobuf data parsing errors.
     */
    @ExceptionHandler(InvalidProtocolBufferException.class)
    public ResponseEntity<String> handleParsingException(InvalidProtocolBufferException ex) {
        log.error("Parsing error occurred: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error: Data parsing failed.");
    }
}