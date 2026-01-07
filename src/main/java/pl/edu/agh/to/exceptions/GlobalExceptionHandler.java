package pl.edu.agh.to.exceptions;

import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex, req, true);
    }

    @ExceptionHandler({
            NotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex, req, true);
    }

    @ExceptionHandler({
            StaticGtfsDownloadException.class,
            StaticGtfsExtractException.class,
            ExternalServiceException.class,
            InvalidProtocolBufferException.class
    })
    public ResponseEntity<ApiError> handleBadGateway(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, ex, req, false);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "Not found",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, req, false);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, Exception ex, HttpServletRequest req, boolean clientError) {
        if (clientError) {
            log.warn("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), ex.getMessage());
        } else {
            log.error("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), ex.getMessage(), ex);
        }

        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                safeMessage(ex, status),
                req.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    private String safeMessage(Exception ex, HttpStatus status) {
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return "Unexpected server error";
        }
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? status.getReasonPhrase() : msg;
    }
}