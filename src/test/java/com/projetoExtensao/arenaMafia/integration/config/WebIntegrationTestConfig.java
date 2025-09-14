package com.projetoExtensao.arenaMafia.integration.config;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class WebIntegrationTestConfig extends BaseTestContainersConfig {

  @LocalServerPort private int port;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }
}
