package com.travelplatform.packages.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.travelplatform.packages.destination.exception.DestinationNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", fieldErrors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePackageNotFound(PackageNotFoundException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBookingNotFound(BookingNotFoundException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(PackageDepartureNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDepartureNotFound(PackageDepartureNotFoundException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(DestinationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDestinationNotFound(DestinationNotFoundException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(PackageNotBookableException.class)
    public ResponseEntity<Map<String, Object>> handlePackageNotBookable(PackageNotBookableException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(InsufficientSlotsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientSlots(InsufficientSlotsException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedBookingActionException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedBookingActionException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Something went wrong: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> notFound(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
