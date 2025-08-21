package com.projetoExtensao.arenaMafia.integration.config;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class TestIntegrationBaseConfig extends TestContainersConfig {

  @LocalServerPort private int port;

  @BeforeEach
  public void setUpRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }
}
