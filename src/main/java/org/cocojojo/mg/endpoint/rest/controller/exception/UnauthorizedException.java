package org.cocojojo.mg.endpoint.rest.controller.exception;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
