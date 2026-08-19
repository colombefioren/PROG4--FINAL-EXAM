package org.cocojojo.mg.endpoint.rest.controller.exception;

public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
