package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.TestIntegrationBaseConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Testes de Integração para AccountVerificationController")
public class AccountVerificationControllerTest extends TestIntegrationBaseConfig {

  @Autowired private UserJpaRepository userJpaRepository;
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
    super.setUpRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/auth")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @AfterEach
  void cleanDatabaseAfterTest() {
    userJpaRepository.deleteAll();
  }

  private void mockPersistUser(
      String username, String fullName, String phone, AccountStatus status) {
    String passwordEncoded = passwordEncoder.encode(defaultPassword);
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            username,
            fullName,
            phone,
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
    void verifyAccount_shouldReturn200AndTokens_whenOtpIsValidForPendingUser() {
      // Arrange
      mockPersistUser(defaultUsername, defaultFullName, defaultPhone, defaultStatus);
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
      assertThat(responseBody).isNotNull();
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
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP estiver ausente ou inválido")
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
      assertThat(response.fieldErrors().get(0).fieldName()).isEqualTo("code");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido")
    void verifyAccount_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      mockPersistUser(defaultUsername, defaultFullName, defaultPhone, defaultStatus);
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
    @DisplayName("Deve retornar 404 Not Found quando o usuário não existir")
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
      mockPersistUser(defaultUsername, defaultFullName, defaultPhone, AccountStatus.ACTIVE);
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
      assertThat(response.message()).isEqualTo("Atenção: A conta já está ativada.");
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
      mockPersistUser(defaultUsername, defaultFullName, defaultPhone, defaultStatus);
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
    @DisplayName("Deve retornar 400 Bad Request quando o telefone estiver ausente ou inválido")
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
      assertThat(response.fieldErrors().get(0).fieldName()).isEqualTo("phone");
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar reenviar código para uma conta já ativa")
    void resendCode_shouldReturn409_whenAccountIsAlreadyActive() {
      // Arrange
      mockPersistUser(defaultUsername, defaultFullName, defaultPhone, AccountStatus.ACTIVE);
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
          .isEqualTo("Atenção: Está conta não está pendente para verificação.");
      assertThat(response.path()).isEqualTo("/api/auth/resend-code");
      assertThat(response.fieldErrors()).isNull();
    }
  }
}
