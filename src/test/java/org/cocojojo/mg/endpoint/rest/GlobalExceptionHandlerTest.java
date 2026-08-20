package org.cocojojo.mg.endpoint.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.NoSuchElementException;
import org.cocojojo.mg.endpoint.rest.controller.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void unauthorizedExceptionMapsTo401() {
    var response = handler.handleUnauthorized(new UnauthorizedException("User not authenticated"));
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void illegalStateExceptionMapsTo500() {
    var response = handler.handleInternal(new IllegalStateException("Unknown user type"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void noSuchElementExceptionMapsTo500() {
    var response = handler.handleInternal(new NoSuchElementException("Missing value"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }
}
