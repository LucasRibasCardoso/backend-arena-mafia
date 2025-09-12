package com.projetoExtensao.arenaMafia.integration.controller.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.*;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para PasswordResetController")
public class PasswordResetControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private PasswordResetTokenPort passwordResetTokenPort;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private OtpSessionPort otpSessionPort;
  @Autowired private OtpPort otpPort;

  private RequestSpecification specification;

  private final String defaultNewPassword = "newPassword123";
  private final String defaultConfirmPassword = "newPassword123";

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
  @DisplayName("Etapa 1: Testes para o endpoint /auth/forgot-password")
  class ForgotPasswordTests {

    @Test
    @DisplayName("Deve retornar 202 Accept quando a redefinição de senha for iniciada com sucesso")
    void forgotPassword_shouldReturn202_whenPhoneExists() {
      // Arrange
      mockPersistUser();
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      ForgotPasswordResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(202)
              .extract()
              .as(ForgotPasswordResponseDto.class);

      // Assert
      assertThat(response.otpSessionId().toString()).hasSize(36); // UUID
      assertThat(response.message())
          .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");
    }

    @Test
    @DisplayName("Deve retornar 202 Accept mesmo quando o telefone não existir")
    void forgotPassword_shouldReturn202_whenPhoneNotExists() {
      // Arrange
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      ForgotPasswordResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(202)
              .extract()
              .as(ForgotPasswordResponseDto.class);

      // Assert
      assertThat(response.otpSessionId().toString()).hasSize(36); // Fake UUID
      assertThat(response.message())
          .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidPhoneProvider
      @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido")
      void forgotPassword_shouldReturn400_whenRequestDtoIsInvalid(
          String invalidPhone, String expectedErrorCode) {
        // Arrange
        var request = new ForgotPasswordRequestDto(invalidPhone);

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/forgot-password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(response.fieldErrors()).isNotEmpty();
        assertThat(response.fieldErrors())
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("phone")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido")
    void validateResetToken_shouldReturn400_whenPhoneIsInvalid() {
      // Arrange
      String invalidPhone = "+999123456789";
      var request = new ForgotPasswordRequestDto(invalidPhone);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.PHONE_INVALID_FORMAT;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }

  @Nested
  @DisplayName("Etapa 2: Testes para o endpoint /auth/reset-password-token")
  class ValidatePasswordResetOtpTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código OTP for válido")
    void validateResetToken_shouldReturn200_whenOtpIsValid() {
      // Arrange
      User mockUser = mockPersistUser();
      OtpCode otpCode = otpPort.generateOtpCode(mockUser.getId());
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

      // Act
      PasswordResetTokenResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password-token")
              .then()
              .statusCode(200)
              .extract()
              .as(PasswordResetTokenResponseDto.class);

      // Assert
      assertThat(response.passwordResetToken().toString()).hasSize(36);
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidOtpSessionIdProvider
      @DisplayName("Deve retornar 400 Bad Request quando o OtpSessionId for inválido")
      void validateResetToken_shouldReturn400_whenOtpSessionIdIsInvalid(
          String otpSessionId, String expectedErrorCode) {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("otpSessionId", otpSessionId);
        requestBody.put("otpCode", "123456");

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(requestBody)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).hasSize(1);
        assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("otpSessionId");
        assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
        assertThat(fieldErrors.getFirst().developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @InvalidOtpCodeProvider
      @DisplayName("Deve retornar 400 Bad Request quando o OtpCode for inválido")
      void validateResetToken_shouldReturn400_whenOtpCodeIsInvalid(
          String invalidOtpCode, String expectedErrorCode) {
        // Arrange
        String otpSessionId = OtpSessionId.generate().toString();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("otpSessionId", otpSessionId);
        requestBody.put("otpCode", invalidOtpCode);

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(requestBody)
                .when()
                .post("/reset-password-token")
                .then()
                .log()
                .all()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(response.fieldErrors()).isNotEmpty();
        assertThat(response.fieldErrors())
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("otpCode")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o OTP for inválido ou expirado")
    void validateResetToken_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      User mockUser = mockPersistUser(AccountStatus.ACTIVE);
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

      OtpCode invalidOtpCode = OtpCode.generate();
      var request = new ValidateOtpRequestDto(otpSessionId, invalidOtpCode);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password-token")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.OTP_CODE_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a sessão OTP for inválida ou expirada")
    void validateResetToken_shouldReturn400_whenOtpSessionIsInvalid() {
      // Arrange
      OtpCode otpCode = OtpCode.generate();
      OtpSessionId invalidOtpSessionId = OtpSessionId.generate();
      var request = new ValidateOtpRequestDto(invalidOtpSessionId, otpCode);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password-token")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.OTP_SESSION_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void validateResetToken_shouldReturn404_whenUserNotFound() {
      // Arrange
      UUID userId = UUID.randomUUID();
      OtpCode otpCode = OtpCode.generate();
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(userId);
      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password-token")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Nested
    @DisplayName("Deve retornar 403 Forbidden quando a conta não está ativada")
    class AccountStateTests {
      @Test
      @DisplayName("Deve retornar 403 Forbidden quando a conta está bloqueada")
      void validateResetToken_shouldReturn403_whenAccountIsLocked() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.LOCKED);
        OtpCode otpCode = otpPort.generateOtpCode(mockUser.getId());
        OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

        var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_LOCKED;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 403 Forbidden quando a conta está pendente de verificação")
      void validateResetToken_shouldReturn403_whenAccountIsNotVerified() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
        OtpCode otpCode = otpPort.generateOtpCode(mockUser.getId());
        OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

        var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_PENDING_VERIFICATION;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 403 Forbidden quando a conta está desativada")
      void validateResetToken_shouldReturn403_whenAccountIsDisabled() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.DISABLED);
        OtpCode otpCode = otpPort.generateOtpCode(mockUser.getId());
        OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

        var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_DISABLED;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Etapa 3: Testes para o endpoint /auth/reset-password")
  class ResetPasswordTests {

    @Test
    @DisplayName("Deve retornar 204 No Content quando o token de redefinição for válido")
    void resetPassword_shouldReturn204_whenTokenIsValid() {
      // Arrange
      User mockUser = mockPersistUser();
      ResetToken token = passwordResetTokenPort.generateToken(mockUser.getId());

      String newPassword = "newpassword";
      var request = new ResetPasswordRequestDto(token, newPassword, newPassword);

      // Act
      given()
          .spec(specification)
          .body(request)
          .when()
          .post("/reset-password")
          .then()
          .statusCode(204);

      // Assert
      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      String updatedPassword = updatedUser.getPasswordHash();

      assertThat(passwordEncoder.matches(newPassword, updatedPassword)).isTrue();
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando às senhas não coincidirem")
      void resetPassword_shouldReturn400_whenPasswordsDoNotMatch() {
        // Arrange
        User mockUser = mockPersistUser();
        ResetToken resetToken = passwordResetTokenPort.generateToken(mockUser.getId());

        var request =
            new ResetPasswordRequestDto(resetToken, defaultNewPassword, "invalidConfirm123");

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.PASSWORDS_DO_NOT_MATCH;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(response.fieldErrors()).hasSize(1);
        assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("confirmPassword");
        assertThat(response.fieldErrors().getFirst().errorCode()).isEqualTo(errorCode.name());
      }

      @InvalidResetTokenProvider
      @DisplayName("Deve retornar 400 Bad Request quando o token de redefinição for inválido")
      void resetPassword_shouldReturn400_whenResetTokenIsInvalid(
          String invalidToken, String expectedErrorCode) {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("passwordResetToken", invalidToken);
        requestBody.put("newPassword", "newPassword123");
        requestBody.put("confirmPassword", "newPassword123");

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(requestBody)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("passwordResetToken")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidPasswordProvider
      @DisplayName("Deve retornar 400 Bad Request quando a nova senha for inválida")
      void resetPassword_shouldReturn400_whenNewPasswordIsInvalid(
          String invalidPassword, String expectedErrorCode) {
        // Arrange
        User mockUser = mockPersistUser();
        ResetToken resetToken = passwordResetTokenPort.generateToken(mockUser.getId());
        var request = new ResetPasswordRequestDto(resetToken, invalidPassword, invalidPassword);

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("newPassword")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request caso a senha de confirmação for nula ou vazia")
      void resetPassword_shouldReturn400_whenConfirmPasswordIsNull() {
        // Arrange
        User mockUser = mockPersistUser();
        ResetToken resetToken = passwordResetTokenPort.generateToken(mockUser.getId());
        var request = new ResetPasswordRequestDto(resetToken, "newPassword", null);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.CONFIRM_PASSWORD_REQUIRED;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).hasSize(1);
        assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("confirmPassword");
        assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
        assertThat(fieldErrors.getFirst().developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Test
    @DisplayName(
        "Deve retornar 400 Bad Request quando o token de redefinição for inválido ou expirado")
    void resetPassword_shouldReturn400_whenTokenIsInvalid() {
      // Arrange
      ResetToken invalidResetToken = ResetToken.generate();
      var request =
          new ResetPasswordRequestDto(
              invalidResetToken, defaultNewPassword, defaultConfirmPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.RESET_TOKEN_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando não for encontrado o usuário do token")
    void resetPassword_shouldReturn404_whenUserNotFound() {
      // Arrange
      UUID userId = UUID.randomUUID();
      ResetToken resetToken = passwordResetTokenPort.generateToken(userId);
      var request =
          new ResetPasswordRequestDto(resetToken, defaultNewPassword, defaultConfirmPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Nested
    @DisplayName("Deve retornar 403 Conflict quando a conta não está ativada")
    class AccountStateTests {

      @Test
      @DisplayName("Deve retornar 403 Conflict quando a conta está pendente de verificação")
      void resetPassword_shouldReturn403_whenAccountIsPending() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
        ResetToken token = passwordResetTokenPort.generateToken(mockUser.getId());
        var request =
            new ResetPasswordRequestDto(token, defaultNewPassword, defaultConfirmPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_PENDING_VERIFICATION;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 403 Conflict quando a conta está bloqueada")
      void resetPassword_shouldReturn403_whenAccountIsLocked() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.LOCKED);
        ResetToken token = passwordResetTokenPort.generateToken(mockUser.getId());
        var request =
            new ResetPasswordRequestDto(token, defaultNewPassword, defaultConfirmPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_LOCKED;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 403 Conflict quando a conta está desativada")
      void resetPassword_shouldReturn403_whenAccountIsDisabled() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.DISABLED);
        ResetToken token = passwordResetTokenPort.generateToken(mockUser.getId());
        var request =
            new ResetPasswordRequestDto(token, defaultNewPassword, defaultConfirmPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_DISABLED;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/reset-password");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
