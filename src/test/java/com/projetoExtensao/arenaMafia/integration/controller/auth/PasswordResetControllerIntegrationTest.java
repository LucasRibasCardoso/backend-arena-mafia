package com.projetoExtensao.arenaMafia.integration.controller.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
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
      assertThat(response.otpSessionId()).hasSize(36); // UUID
      assertThat(response.message())
          .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");
    }

    @Test
    @DisplayName(
        "Deve retornar 202 Accept quando o telefone não existir e o código não for enviado")
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
      assertThat(response.otpSessionId()).hasSize(36); // Fake UUID
      assertThat(response.message())
          .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido no DTO")
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
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando a conta não está ativada")
    class AccountStateTests {
      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está bloqueada")
      void forgotPassword_shouldReturn409_whenAccountIsLocked() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.LOCKED);
        var request = new ForgotPasswordRequestDto(mockUser.getPhone());

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
        assertThat(response.message())
            .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está pendente de verificação")
      void forgotPassword_shouldReturn409_whenAccountIsNotVerified() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
        var request = new ForgotPasswordRequestDto(mockUser.getPhone());

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
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está desativada")
      void forgotPassword_shouldReturn409_whenAccountIsDisabled() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.DISABLED);
        var request = new ForgotPasswordRequestDto(mockUser.getPhone());

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
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");
      }
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
      String otpCode = otpPort.generateAndSaveOtp(mockUser.getId());
      String otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

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
      assertThat(response.passwordResetToken()).hasSize(36);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido no DTO")
    void validateResetToken_shouldReturn400_whenRequestDtoIsInvalid() {
      // Arrange
      String otpSessionId = UUID.randomUUID().toString();
      var request = new ValidateOtpRequestDto(otpSessionId, "");

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
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.path()).isEqualTo("/api/auth/reset-password-token");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("code");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o OTP for inválido")
    void validateResetToken_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      User mockUser = mockPersistUser(AccountStatus.ACTIVE);
      String otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

      String invalidCodeOTP = "000000";
      var request = new ValidateOtpRequestDto(otpSessionId, invalidCodeOTP);

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
      assertThat(response.message()).isEqualTo("Código de verificação inválido ou expirado.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a sessão OTP for inválida")
    void validateResetToken_shouldReturn400_whenOtpSessionIsInvalid() {
      // Arrange
      String invalidOtpSessionId = UUID.randomUUID().toString();
      var request = new ValidateOtpRequestDto(invalidOtpSessionId, "123456");

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
      assertThat(response.message()).isEqualTo("Sessão de verificação inválida ou expirada.");
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void validateResetToken_shouldReturn404_whenUserNotFound() {
      // Arrange
      String otpSessionId = otpSessionPort.generateOtpSession(UUID.randomUUID());
      var request = new ValidateOtpRequestDto(otpSessionId, "123456");

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
      assertThat(response.message()).isEqualTo("Usuário não encontrado.");
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando a conta não está ativada")
    class AccountStateTests {
      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está bloqueada")
      void validateResetToken_shouldReturn409_whenAccountIsLocked() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.LOCKED);
        String otpCode = otpPort.generateAndSaveOtp(mockUser.getId());
        String otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

        var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está pendente de verificação")
      void validateResetToken_shouldReturn409_whenAccountIsNotVerified() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
        String otpCode = otpPort.generateAndSaveOtp(mockUser.getId());
        String otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

        var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está desativada")
      void validateResetToken_shouldReturn409_whenAccountIsDisabled() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.DISABLED);
        String otpCode = otpPort.generateAndSaveOtp(mockUser.getId());
        String otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

        var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/reset-password-token")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");
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
      String token = passwordResetTokenPort.generateToken(mockUser.getId());

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

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando às senhas não coincidirem")
    void resetPassword_shouldReturn400_whenPasswordsDoNotMatch() {
      // Arrange
      User mockUser = mockPersistUser();
      String resetToken = passwordResetTokenPort.generateToken(mockUser.getId());

      String differentConfirmPassword = "invalidConfirm123";
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
      assertThat(response.message()).isEqualTo("Token inválido ou expirado.");
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando não for encontrado o usuário do token")
    void resetPassword_shouldReturn404_whenUserNotFound() {
      // Arrange
      String resetToken = passwordResetTokenPort.generateToken(UUID.randomUUID());
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
      assertThat(response.message())
          .isEqualTo(
              "Ocorreu um erro ao redefinir sua senha. Por favor, inicie o processo novamente.");
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando a conta não está ativada")
    class AccountStateTests {
      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está pendente de verificação")
      void resetPassword_shouldReturn409_whenAccountIsPending() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
        String token = passwordResetTokenPort.generateToken(mockUser.getId());
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
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está bloqueada")
      void resetPassword_shouldReturn409_whenAccountIsLocked() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.LOCKED);
        String token = passwordResetTokenPort.generateToken(mockUser.getId());
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
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está desativada")
      void resetPassword_shouldReturn409_whenAccountIsDisabled() {
        // Arrange
        User mockUser = mockPersistUser(AccountStatus.DISABLED);
        String token = passwordResetTokenPort.generateToken(mockUser.getId());
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
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");
      }
    }
  }
}
