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
  void specShouldServeTheSwaggerUiPage() {
    webTestClient
        .get()
        .uri("/spec")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType("text/html")
        .expectBody(String.class)
        .value(containsString("swagger-ui"))
        .value(containsString("/openapi.yaml"));
  }

  @Test
  void rawSpecShouldBeServedAsYaml() {
    webTestClient
        .get()
        .uri("/openapi.yaml")
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
  void specAndRawYamlDoNotRequireAuthentication() {
    webTestClient.get().uri("/spec").exchange().expectStatus().isOk();
    webTestClient.get().uri("/openapi.yaml").exchange().expectStatus().isOk();
  }

  @Test
  void generatedApiDocsShouldStillBeServed() {
    webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
  }
}
