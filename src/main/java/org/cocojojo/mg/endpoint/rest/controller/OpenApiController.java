package org.cocojojo.mg.endpoint.rest.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {

  private static final String SPEC_PATH = "openapi.yaml";
  private static final String UI_PATH = "spec-ui.html";
  private static final MediaType YAML = MediaType.parseMediaType("application/yaml");

  @GetMapping(value = "/spec", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Resource> specUi() {
    var page = new ClassPathResource(UI_PATH);
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
  }

  @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
  public ResponseEntity<Resource> openApiSpec() {
    var spec = new ClassPathResource(SPEC_PATH);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + SPEC_PATH + "\"")
        .contentType(YAML)
        .body(spec);
  }
}
