package org.cocojojo.mg.endpoint.rest.controller.exception;

/**
 * Raised when a payload violates a HEI business rule (credit totals, exam coefficients, track
 * compatibility...).
 */
public class InvalidCurriculumException extends RuntimeException {
  public InvalidCurriculumException(String message) {
    super(message);
  }
}
