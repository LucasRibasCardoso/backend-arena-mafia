package com.projetoExtensao.arenaMafia.integration.controller.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AccountVerificationController")
public class AccountVerificationControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private UserRepositoryPort userRepository;
  @Autowired private OtpPort otpPort;

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

  @Nested
  @DisplayName("Testes para o endpoint /auth/verify-account")
  class VerifyAccountTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código OTP for válido e a conta estiver pendente")
    void verifyAccount_shouldReturn200_whenOtpIsValidForPendingUser() {
      // Arrange
      User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
      String codeOTP = otpPort.generateCodeOTP(mockUser.getId());
      var request = new ValidateOtpRequestDto(mockUser.getId().toString(), codeOTP);

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
      assertThat(responseBody.phone()).isEqualTo(mockUser.getPhone());
      assertThat(responseBody.username()).isEqualTo(mockUser.getUsername());
      assertThat(responseBody.fullName()).isEqualTo(mockUser.getFullName());
      assertThat(responseBody.role()).isEqualTo(mockUser.getRole().name());
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie.getValue()).hasSize(36);

      User activatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(activatedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTO for vazio")
    void verifyAccount_shouldReturn400_whenOtpIsMissing() {
      // Arrange
      String userId = UUID.randomUUID().toString();
      var request = new ValidateOtpRequestDto(userId, "");

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
    @DisplayName("Deve retornar 400 Bad Request quando o userId for inválido")
    void validateResetToken_shouldReturn400_whenUserIdIsInvalid() {
      // Arrange
      String userId = "invalid-uuid";
      var request = new ValidateOtpRequestDto(userId, "123456");

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
      assertThat(response.message()).isEqualTo("Identificador de usuário inválido.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido")
    void verifyAccount_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      User mockUser = mockPersistUser();
      String invalidCodeOTP = "111222";
      var request = new ValidateOtpRequestDto(mockUser.getId().toString(), invalidCodeOTP);

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
      String userId = UUID.randomUUID().toString();
      var request = new ValidateOtpRequestDto(userId, "123456");

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
      User mockUser = mockPersistUser();

      String codeOTP = otpPort.generateCodeOTP(mockUser.getId());
      var request = new ValidateOtpRequestDto(mockUser.getId().toString(), codeOTP);

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
          .isEqualTo("Não é possível ativar uma conta que não está pendente de verificação.");
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/resend-code")
  class ResendCodeTests {

    @Test
    @DisplayName("Deve retornar 204 No Content quando o código for reenviado com sucesso")
    void resendCode_shouldReturn204_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
      ResendCodeRequestDto request = new ResendCodeRequestDto(mockUser.getId().toString());

      // Act
      given().spec(specification).body(request).when().post("/resend-code").then().statusCode(204);
    }

    @Test
    @DisplayName("Deve retornar 204 No Content mesmo quando o usuário não for encontrado")
    void resendCode_shouldReturn204_whenUserNotFound() {
      // Arrange
      String userId = UUID.randomUUID().toString();
      var request = new ResendCodeRequestDto(userId);

      // Act
      given().spec(specification).body(request).when().post("/resend-code").then().statusCode(204);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o userId for vazio")
    void resendCode_shouldReturn400_whenUserIdIsBlank() {
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
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("userId");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o userId for inválido")
    void validateResetToken_shouldReturn400_whenUserIdIsInvalid() {
      // Arrange
      String userId = "invalid-uuid";
      var request = new ResendCodeRequestDto(userId);

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
      assertThat(response.message()).isEqualTo("Identificador de usuário inválido.");
      assertThat(response.path()).isEqualTo("/api/auth/resend-code");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar reenviar código para uma conta já ativa")
    void resendCode_shouldReturn409_whenAccountIsAlreadyActive() {
      // Arrange
      User mockUser = mockPersistUser();
      var request = new ResendCodeRequestDto(mockUser.getId().toString());

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
