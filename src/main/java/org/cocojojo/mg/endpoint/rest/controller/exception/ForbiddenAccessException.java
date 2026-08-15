package org.cocojojo.mg.endpoint.rest.controller.exception;

public class ForbiddenAccessException extends RuntimeException {
  public ForbiddenAccessException(String message) {
    super(message);
  }
}
