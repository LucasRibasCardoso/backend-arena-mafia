package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para PasswordResetController")
public class PasswordResetControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private PasswordResetTokenPort passwordResetTokenPort;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private OtpPort otpPort;

  private RequestSpecification specification;

  private final String defaultPhone = "+558320548186";
  private final String defaultUsername = "test_user";
  private final String defaultFullName = "Test User";
  private final String defaultPassword = "123456";
  private final AccountStatus defaultStatus = AccountStatus.ACTIVE;

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
  @DisplayName("Etapa 1: Testes para o endpoint /auth/forgot-password")
  class ForgotPasswordTests {

    @Test
    @DisplayName("Deve retornar 202 Accept quando o telefone existir")
    void forgotPassword_shouldReturn202_whenPhoneExists() {
      // Arrange
      mockPersistUser(defaultStatus);
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      SimpleMessageResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(202)
              .extract()
              .as(SimpleMessageResponseDto.class);

      // Assert
      assertThat(response.message())
          .isEqualTo(
              "Se o telefone informado for válido, enviaremos um código para verificação da conta.");
    }

    @Test
    @DisplayName("Deve retornar 202 Accept quando o telefone não existir")
    void forgotPassword_shouldReturn202_whenPhoneNotExists() {
      // Arrange
      mockPersistUser(defaultStatus);
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      SimpleMessageResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(202)
              .extract()
              .as(SimpleMessageResponseDto.class);

      // Assert
      assertThat(response.message())
          .isEqualTo(
              "Se o telefone informado for válido, enviaremos um código para verificação da conta.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o request DTO for inválido")
    void forgotPassword_shouldReturn400_whenRequestDtoIsInvalid() {
      // Arrange
      var request = new ForgotPasswordRequestDto("invalid-phone");

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

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("phone");
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
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver bloqueada")
    void forgotPassword_shouldReturn409_whenAccountIsLocked() {
      // Arrange
      mockPersistUser(AccountStatus.LOCKED);
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName(
        "Deve retornar 409 Conflict quando a conta do usuário estiver pendente de verificação")
    void forgotPassword_shouldReturn409_whenAccountIsNotVerified() {
      // Arrange
      mockPersistUser(AccountStatus.PENDING_VERIFICATION);
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver desativada")
    void forgotPassword_shouldReturn409_whenAccountIsDisabled() {
      // Arrange
      mockPersistUser(AccountStatus.DISABLED);
      var request = new ForgotPasswordRequestDto(defaultPhone);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/forgot-password")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Atenção: Sua conta está desativada.");
      assertThat(response.path()).isEqualTo("/api/auth/forgot-password");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Etapa 2: Testes para o endpoint /auth/password-reset-token")
  class GeneratePasswordResetTokenTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código OTP for válido")
    void validateResetToken_shouldReturn200_whenOtpIsValid() {
      // Arrange
      mockPersistUser(defaultStatus);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String codeOTP = otpPort.generateCodeOTP(user.getId());
      var request = new ValidateOtpRequestDto(defaultPhone, codeOTP);

      // Act
      PasswordResetTokenResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(200)
              .extract()
              .as(PasswordResetTokenResponseDto.class);

      // Assert
      assertThat(response.passwordResetToken()).hasSize(36);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o request DTO for inválido")
    void validateResetToken_shouldReturn400_whenRequestDtoIsInvalid() {
      // Arrange
      var request = new ValidateOtpRequestDto(defaultPhone, "");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
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
              .post("/password-reset-token")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o OTP for inválido")
    void validateResetToken_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      mockPersistUser(defaultStatus);
      String invalidCodeOTP = "000000";
      var request = new ValidateOtpRequestDto(defaultPhone, invalidCodeOTP);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("Código de verificação inválido ou expirado.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void validateResetToken_shouldReturn404_whenUserNotFound() {
      // Arrange
      var request = new ValidateOtpRequestDto(defaultPhone, defaultPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.message())
          .isEqualTo(
              "Usuário não encontrado. Verifique o número de telefone informado e tente novamente.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver bloqueada")
    void validateResetToken_shouldReturn409_whenAccountIsLocked() {
      // Arrange
      mockPersistUser(AccountStatus.LOCKED);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String codeOTP = otpPort.generateCodeOTP(user.getId());
      var request = new ValidateOtpRequestDto(defaultPhone, codeOTP);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName(
        "Deve retornar 409 Conflict quando a conta do usuário estiver pendente de verificação")
    void validateResetToken_shouldReturn409_whenAccountIsNotVerified() {
      // Arrange
      mockPersistUser(AccountStatus.PENDING_VERIFICATION);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String codeOTP = otpPort.generateCodeOTP(user.getId());
      var request = new ValidateOtpRequestDto(defaultPhone, codeOTP);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver desativada")
    void validateResetToken_shouldReturn409_whenAccountIsDisabled() {
      // Arrange
      mockPersistUser(AccountStatus.DISABLED);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String codeOTP = otpPort.generateCodeOTP(user.getId());
      var request = new ValidateOtpRequestDto(defaultPhone, codeOTP);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/password-reset-token")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Atenção: Sua conta está desativada.");
      assertThat(response.path()).isEqualTo("/api/auth/password-reset-token");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Etapa 3: Testes para o endpoint /auth/reset-password")
  class ResetPasswordTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o token de redefinição for válido")
    void resetPassword_shouldReturn200_whenTokenIsValid() {
      // Arrange
      mockPersistUser(AccountStatus.ACTIVE);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String passwordResetToken = passwordResetTokenPort.save(user.getId());
      String newPassword = "password123";
      String confirmPassword = "password123";
      var request = new ResetPasswordRequestDto(passwordResetToken, newPassword, confirmPassword);

      // Act
      SimpleMessageResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password")
              .then()
              .statusCode(200)
              .extract()
              .as(SimpleMessageResponseDto.class);

      // Assert
      assertThat(response.message())
          .isEqualTo(
              "Senha redefinida com sucesso. Você já pode fazer o login com sua nova senha.");

      User updatedUser = userRepository.findById(user.getId()).orElseThrow();
      String updatedPasswordHash = updatedUser.getPasswordHash();

      assertThat(updatedPasswordHash).isNotEqualTo(newPassword);
      assertThat(passwordEncoder.matches(newPassword, updatedPasswordHash)).isTrue();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando às senhas não coincidirem")
    void resetPassword_shouldReturn400_whenPasswordsDoNotMatch() {
      // Arrange
      mockPersistUser(defaultStatus);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String resetToken = passwordResetTokenPort.save(user.getId());
      String differentConfirmPassword = "differentPass456";
      var request =
          new ResetPasswordRequestDto(resetToken, defaultNewPassword, differentConfirmPassword);

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
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("confirmPassword");
      assertThat(response.fieldErrors().getFirst().message())
          .isEqualTo("A senha de confirmação não corresponde à nova senha.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o token de redefinição for inválido")
    void resetPassword_shouldReturn400_whenTokenIsInvalid() {
      // Arrange
      String invalidResetToken = UUID.randomUUID().toString();
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
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("Token inválido ou expirado.");
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName(
        "Deve retornar 404 Not Found quando o token de redefinição não estiver associado a nenhum usuário.")
    void resetPassword_shouldReturn404_whenUserNotFound() {
      // Arrange
      mockPersistUser(defaultStatus);
      String resetToken = passwordResetTokenPort.save(UUID.randomUUID());
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
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.message())
          .isEqualTo(
              "Ocorreu um erro ao redefinir sua senha. Por favor, inicie o processo novamente.");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário não estiver verificada")
    void resetPassword_shouldReturn409_whenAccountIsNotVerified() {
      // Arrange
      mockPersistUser(AccountStatus.PENDING_VERIFICATION);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String passwordResetToken = passwordResetTokenPort.save(user.getId());
      var request =
          new ResetPasswordRequestDto(
              passwordResetToken, defaultNewPassword, defaultConfirmPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver bloqueada")
    void resetPassword_shouldReturn409_whenAccountIsLocked() {
      // Arrange
      mockPersistUser(AccountStatus.LOCKED);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String passwordResetToken = passwordResetTokenPort.save(user.getId());
      var request =
          new ResetPasswordRequestDto(
              passwordResetToken, defaultNewPassword, defaultConfirmPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message())
          .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver desativada")
    void resetPassword_shouldReturn409_whenAccountIsDisabled() {
      // Arrange
      mockPersistUser(AccountStatus.DISABLED);
      User user = userRepository.findByUsername(defaultUsername).orElseThrow();
      String passwordResetToken = passwordResetTokenPort.save(user.getId());
      var request =
          new ResetPasswordRequestDto(
              passwordResetToken, defaultNewPassword, defaultConfirmPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/reset-password")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Atenção: Sua conta está desativada.");
      assertThat(response.path()).isEqualTo("/api/auth/reset-password");
      assertThat(response.fieldErrors()).isNull();
    }
  }
}
