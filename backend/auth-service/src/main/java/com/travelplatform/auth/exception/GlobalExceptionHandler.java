package com.travelplatform.auth.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.travelplatform.auth.dto.UserAdminResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Triggered when @Valid fails on a @RequestBody DTO.
	 * Returns a map of field -> error message.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> fieldErrors = new HashMap<>();

		ex.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.put(error.getField(), error.getDefaultMessage())
		);

		response.put("success", false);
		response.put("message", "Validation failed");
		response.put("errors", fieldErrors);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Triggered when a registration request uses an already-registered email.
	 */
	@ExceptionHandler(com.travelplatform.auth.exception.EmailAlreadyExistsException.class)
	public ResponseEntity<Map<String, Object>> handleEmailExists(com.travelplatform.auth.exception.EmailAlreadyExistsException ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Triggered when a refresh token is missing, expired, or revoked.
	 */
	@ExceptionHandler(com.travelplatform.auth.exception.TokenRefreshException.class)
	public ResponseEntity<Map<String, Object>> handleTokenRefresh(com.travelplatform.auth.exception.TokenRefreshException ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	/**
	 * Triggered on invalid email/password during login.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<UserAdminResponse> handleBadCredentials(BadCredentialsException ex) {
		UserAdminResponse response = new UserAdminResponse();
		response.setSuccess(false);
		response.setMessage(ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Triggered when a path variable like {busid}/{userid} can't be parsed
	 * (e.g. non-numeric value passed where a Long is expected).
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Catch-all for anything else so the client never sees a raw stack trace.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", "Something went wrong: " + ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
