package org.cocojojo.mg.endpoint.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ForbiddenAccessException.class)
  public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenAccessException ex) {
    return error(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler({InvalidCurriculumException.class, IllegalArgumentException.class})
  public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException ex) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
    var body = new HashMap<String, String>();
    body.put("error", message);
    body.put("status", status.toString());
    return ResponseEntity.status(status).body(body);
  }
}
