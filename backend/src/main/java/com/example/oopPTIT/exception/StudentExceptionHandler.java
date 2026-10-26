package com.example.oopPTIT.exception;

import com.example.oopPTIT.controller.AdminStudentController;
import com.example.oopPTIT.controller.AdminLecturerController;
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

@RestControllerAdvice(assignableTypes = {AdminStudentController.class, AdminLecturerController.class})
public class StudentExceptionHandler {

    @ExceptionHandler({DuplicateStudentException.class, DuplicateLecturerException.class})
    public ResponseEntity<ResponseError> handleDuplicate(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ResponseError(409, exception.getMessage()));
    }

    // Database constraints also protect against concurrent requests passing the pre-checks.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseError> handleIntegrityViolation() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ResponseError(409, "Data conflicts with database constraints"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseError(404, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseError> handleInvalidParameter() {
        return ResponseEntity.badRequest().body(new ResponseError(400, "Invalid request parameter"));
    }

    @ExceptionHandler({InvalidStudentFilterException.class, InvalidLecturerFilterException.class})
    public ResponseEntity<ResponseError> handleInvalidFilter(RuntimeException exception) {
        return ResponseEntity.badRequest().body(new ResponseError(400, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ResponseError(400, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseError> handleUnreadableBody() {
        return ResponseEntity.badRequest()
                .body(new ResponseError(400, "Invalid JSON body or field format"));
    }
}
