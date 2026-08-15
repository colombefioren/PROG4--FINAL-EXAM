package org.cocojojo.mg.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "JWT_SECRET", () -> "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
    registry.add("JWT_EXPIRATION", () -> "3600000");
  }
}
