package com.example.oopPTIT.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.example.oopPTIT.dto.response.ResponseError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class StudentExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseError> handleAuthentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(new ResponseError(401, "Invalid credentials or authentication is required"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseError> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseError(403, "Access denied"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseError> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ResponseError(exception.getStatusCode().value(),
                        exception.getReason() == null ? "Request failed" : exception.getReason()));
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ResponseError> handleBadRequest() {
        return ResponseEntity.badRequest().body(new ResponseError(400, "Invalid request parameters"));
    }

    @ExceptionHandler({DuplicateStudentException.class, DuplicateLecturerException.class})
    public ResponseEntity<ResponseError> handleDuplicate(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ResponseError(HttpStatus.CONFLICT.value(), exception.getMessage()));
    }

    // Database constraints also protect against concurrent requests passing the pre-checks.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseError> handleIntegrityViolation() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ResponseError(HttpStatus.CONFLICT.value(), "Data conflicts with database constraints"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseError(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseError> handleInvalidParameter() {
        return ResponseEntity.badRequest().body(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Invalid request parameter"));
    }

    @ExceptionHandler({InvalidStudentFilterException.class, InvalidLecturerFilterException.class})
    public ResponseEntity<ResponseError> handleInvalidFilter(RuntimeException exception) {
        return ResponseEntity.badRequest().body(new ResponseError(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ResponseError(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseError> handleUnreadableBody() {
        return ResponseEntity.badRequest()
                .body(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Invalid JSON body or field format"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseError> handleUnexpectedError(Exception exception) {
        if (exception instanceof ErrorResponse error) {
            HttpStatus status = HttpStatus.resolve(error.getStatusCode().value());
            return ResponseEntity.status(error.getStatusCode()).body(new ResponseError(error.getStatusCode().value(),
                    status == null ? "Request failed" : status.getReasonPhrase()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseError(500, "An unexpected error occurred"));
    }
}
