package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AccountVerificationController")
public class AccountVerificationControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private OtpPort otpPort;

  private RequestSpecification specification;

  private final String defaultPhone = "+558320548186";
  private final String defaultUsername = "test_user";
  private final String defaultFullName = "Test User";
  private final String defaultPassword = "123456";
  private final RoleEnum defaultRole = RoleEnum.ROLE_USER;
  private final AccountStatus defaultStatus = AccountStatus.PENDING_VERIFICATION;

  @BeforeEach
  void setup() {
    super.setupRestAssured();
    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/auth")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  private void mockPersistUser(AccountStatus status) {
    String passwordEncoded = passwordEncoder.encode(defaultPassword);
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            defaultUsername,
            defaultFullName,
            defaultPhone,
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            Instant.now());

    userRepository.save(user);
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/verify-account")
  class VerifyAccountTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código OTP for válido e a conta estiver pendente")
    void verifyAccount_shouldReturn200_whenOtpIsValidForPendingUser() {
      // Arrange
      mockPersistUser(defaultStatus);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();

      String codeOTP = otpPort.generateCodeOTP(user.getId());
      var request = new ValidateOtpRequestDto(defaultPhone, codeOTP);

      // Act
      Response response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(200)
              .extract()
              .response();
      TokenResponseDto responseBody = response.as(TokenResponseDto.class);

      // Assert
      assertThat(responseBody.phone()).isEqualTo(defaultPhone);
      assertThat(responseBody.username()).isEqualTo(defaultUsername);
      assertThat(responseBody.fullName()).isEqualTo(defaultFullName);
      assertThat(responseBody.role()).isEqualTo(defaultRole.name());
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie.getValue()).hasSize(36);

      User activatedUser = userRepository.findById(user.getId()).orElseThrow();
      assertThat(activatedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o request DTO for inválido")
    void verifyAccount_shouldReturn400_whenOtpIsMissing() {
      // Arrange
      var request = new ValidateOtpRequestDto(defaultPhone, "");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("code");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido")
    void validateResetToken_shouldReturn400_whenPhoneIsInvalid() {
      // Arrange
      String invalidPhone = "+999123456789";
      var request = new ValidateOtpRequestDto(invalidPhone, defaultPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido")
    void verifyAccount_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      mockPersistUser(defaultStatus);
      String invalidCodeOTP = "111222";
      var request = new ValidateOtpRequestDto(defaultPhone, invalidCodeOTP);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("Código de verificação inválido ou expirado.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void verifyAccount_shouldReturn404_whenUserNotFound() {
      // Arrange
      var request = new ValidateOtpRequestDto(defaultPhone, defaultPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.message())
          .isEqualTo(
              "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar ativar uma conta que já está ativa")
    void verifyAccount_shouldReturn409_whenAccountIsAlreadyActive() {
      // Arrange
      mockPersistUser(AccountStatus.ACTIVE);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();

      String codeOTP = otpPort.generateCodeOTP(user.getId());
      var request = new ValidateOtpRequestDto(defaultPhone, codeOTP);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo("Atenção: A conta já está ativada. Você pode fazer login.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/resend-code")
  class ResendCodeTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código for reenviado com sucesso")
    void resendCode_shouldReturn200_whenSuccessful() {
      // Arrange
      mockPersistUser(defaultStatus);
      ResendCodeRequestDto request = new ResendCodeRequestDto(defaultPhone);

      // Act
      SimpleMessageResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(200)
              .extract()
              .as(SimpleMessageResponseDto.class);

      // Assert
      assertThat(response.message()).isEqualTo("Código de verificação reenviado com sucesso.");
    }

    @Test
    @DisplayName("Deve retornar 200 OK mesmo quando o telefone não existir, por segurança")
    void resendCode_shouldReturn200_whenPhoneDoesNotExist() {
      // Arrange
      String nonExistentPhone = "+558320548181";
      var request = new ResendCodeRequestDto(nonExistentPhone);

      // Act
      SimpleMessageResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(200)
              .extract()
              .as(SimpleMessageResponseDto.class);

      // Assert
      assertThat(response.message()).isEqualTo("Código de verificação reenviado com sucesso.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o request DTO for inválido")
    void resendCode_shouldReturn400_whenPhoneIsMissingOrInvalid() {
      // Arrange
      var request = new ResendCodeRequestDto("");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.path()).isEqualTo("/api/auth/resend-code");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("phone");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido")
    void validateResetToken_shouldReturn400_whenPhoneIsInvalid() {
      // Arrange
      String invalidPhone = "+999123456789";
      var request = new ResendCodeRequestDto(invalidPhone);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      assertThat(response.path()).isEqualTo("/api/auth/resend-code");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar reenviar código para uma conta já ativa")
    void resendCode_shouldReturn409_whenAccountIsAlreadyActive() {
      // Arrange
      mockPersistUser(AccountStatus.ACTIVE);
      var request = new ResendCodeRequestDto(defaultPhone);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo(
              "Atenção: Só é possível reenviar o código para contas pendentes de verificação.");
      assertThat(response.path()).isEqualTo("/api/auth/resend-code");
      assertThat(response.fieldErrors()).isNull();
    }
  }
}
