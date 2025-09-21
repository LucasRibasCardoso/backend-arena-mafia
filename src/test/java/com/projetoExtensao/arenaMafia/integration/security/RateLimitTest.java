package com.projetoExtensao.arenaMafia.integration.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "resilience4j.ratelimiter.enabled=true")
public class RateLimitTest extends WebIntegrationTestConfig {

  @Autowired private OtpSessionPort otpSessionPort;

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    super.setupRestAssured();
    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/auth")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Test
  @DisplayName("Deve retornar 429 Too Many Requests quando o limite de tentativas for excedido")
  void login_shouldReturn429_whenRateLimitExceeded() {
    // Arrange
    LoginRequestDto request = new LoginRequestDto("user", "password");
    int maxAttempts = 5; // Deve corresponder ao configurado na aplicação

    // Act
    ErrorResponseDto response = null;
    for (int i = 0; i < maxAttempts + 1; i++) {
      response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .extract()
              .as(ErrorResponseDto.class);
    }

    // Assert
    ErrorCode errorCode = ErrorCode.TOO_MANY_LOGIN_ATTEMPTS;

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(429);
    assertThat(response.path()).isEqualTo("/api/auth/login");
    assertThat(response.errorCode()).isEqualTo(errorCode.name());
    assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
  }

  @Test
  @DisplayName("Deve retornar 429 Too Many Requests quando o limite de taxa for excedido")
  void resendOtp_shouldReturn429_whenRateLimitExceeded() {
    // Arrange
    User mockUser = mockPersistUser();
    OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());
    var request = new ResendOtpRequestDto(otpSessionId);

    // Act
    for (int i = 0; i < 10; i++) {
      given().spec(specification).body(request).when().post("/resend-otp").then().statusCode(204);
    }

    ErrorResponseDto response =
        given()
            .spec(specification)
            .body(request)
            .when()
            .post("/resend-otp")
            .then()
            .statusCode(429)
            .extract()
            .as(ErrorResponseDto.class);

    // Assert
    ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;

    assertThat(response.status()).isEqualTo(429);
    assertThat(response.path()).isEqualTo("/api/auth/resend-otp");
    assertThat(response.errorCode()).isEqualTo(errorCode.name());
    assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
  }
}
