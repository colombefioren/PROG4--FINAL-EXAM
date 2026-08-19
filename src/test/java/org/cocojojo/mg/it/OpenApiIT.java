package org.cocojojo.mg.it;

import static org.hamcrest.Matchers.containsString;

import org.cocojojo.mg.conf.FacadeIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

class OpenApiIT extends FacadeIT {

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void customSpecShouldBeServedPubliclyAsYaml() {
    webTestClient
        .get()
        .uri("/spec")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType("application/yaml")
        .expectBody(String.class)
        .value(containsString("openapi: 3.0.1"))
        .value(containsString("/auth/login"));
  }

  @Test
  void generatedApiDocsShouldStillBeServed() {
    webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
  }

  @Test
  void customSpecDoesNotRequireAuthentication() {
    webTestClient
        .get()
        .uri("/spec")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(containsString("openapi: 3.0.1"));
  }
}
