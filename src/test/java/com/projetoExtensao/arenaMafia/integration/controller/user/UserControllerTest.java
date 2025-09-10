package com.projetoExtensao.arenaMafia.integration.controller.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.response.UserProfileResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.*;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para UserController")
public class UserControllerTest extends WebIntegrationTestConfig {

  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private PendingPhoneChangePort pendingPhoneChangePort;
  @Autowired private OtpPort otpPort;

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    super.setupRestAssured();
    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/users/me")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Nested
  @DisplayName("Teste para o endpoint /api/users/me")
  class GetMyProfileTest {

    @Test
    @DisplayName("Deve retornar 200 OK com os detalhes do perfil do usuário")
    void getMyProfile_shouldReturn200_withUserProfileDetails() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(UserProfileResponseDto.class);

      assertThat(response.username()).isEqualTo(mockUser.getUsername());
      assertThat(response.fullName()).isEqualTo(mockUser.getFullName());
      assertThat(response.phone()).isEqualTo(mockUser.getPhone());
      assertThat(response.role()).isEqualTo(mockUser.getRole().name());
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /api/users/me/profile")
  class UpdateProfileTest {

    @Test
    @DisplayName("Deve retornar 200 OK quando a atualização for bem-sucedida")
    void updateProfile_shouldReturn200_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new UpdateProfileRequestDto("Novo Nome Completo");

      // Act & Assert
      given()
          .spec(specification)
          .header("Authorization", "Bearer " + tokens.accessToken())
          .body(request)
          .when()
          .patch("/profile")
          .then()
          .statusCode(200);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(updatedUser.getFullName()).isEqualTo(request.fullName());
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidFullNameProvider
      @DisplayName("Deve retornar 400 Bad Request quando o nome completo for inválido no DTO")
      void updateProfile_shouldReturn400_whenFullNameIsEmptyOrNull(
          String fullName, String expectedErrorCode) {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new UpdateProfileRequestDto(fullName);

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .patch("/profile")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/profile");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("fullName")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }
  }

  @Nested
  @DisplayName("Teste para o endpoint /api/users/me/username")
  class ChangeUsernameTest {

    @Test
    @DisplayName("Deve retornar 200 OK quando o nome de usuário for alterado com sucesso")
    void changeUsername_shouldReturn200_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangeUsernameRequestDto("new_username");

      // Act & Assert
      given()
          .spec(specification)
          .header("Authorization", "Bearer " + tokens.accessToken())
          .body(request)
          .when()
          .patch("/username")
          .then()
          .statusCode(200);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(updatedUser.getUsername()).isEqualTo(request.username());
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidUsernameProvider
      @DisplayName("Deve retornar 400 Bad Request quando o nome de usuário for inválido no DTO")
      void changeUsername_shouldReturn400_whenUsernameIsEmptyOrNull(
          String username, String expectedErrorCode) {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new ChangeUsernameRequestDto(username);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .patch("/username")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/username");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("username")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando o nome de usuário já estiver em uso")
    void changeUsername_shouldReturn409_whenUsernameAlreadyExists() {
      // Arrange
      mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      User mockExistingUser =
          mockPersistUser("existing_user", "Existing User", "+5521921340987", "123456");

      var request = new ChangeUsernameRequestDto(mockExistingUser.getUsername());

      // Act & Assert
      ErrorResponseDto response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .patch("/username")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.USERNAME_ALREADY_EXISTS;

      assertThat(response.status()).isEqualTo(409);
      assertThat(response.path()).isEqualTo("/api/users/me/username");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }

  @Nested
  @DisplayName("Teste para o endpoint /api/users/me/password")
  class ChangePasswordTest {
    @Test
    @DisplayName("Deve retornar 204 No Content quando a alteração de senha for bem-sucedida")
    void changePassword_shouldReturn204_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangePasswordRequestDto(defaultPassword, "new_password", "new_password");

      // Act & Assert
      given()
          .spec(specification)
          .header("Authorization", "Bearer " + tokens.accessToken())
          .body(request)
          .when()
          .post("/password")
          .then()
          .statusCode(204);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(passwordEncoder.matches("new_password", updatedUser.getPasswordHash())).isTrue();
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando às senhas não se corresponderem")
      void changePassword_shouldReturn400_whenNewPasswordAndConfirmationDoNotMatch() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new ChangePasswordRequestDto(defaultPassword, "new_password", "password");

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PASSWORDS_DO_NOT_MATCH;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("confirmPassword")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidPasswordProvider
      @DisplayName("Deve retornar 400 Bad Request quando a senha atual for inválida")
      void changePassword_shouldReturn400_whenCurrentPasswordIsInvalid(
          String currentPassword, String expectedErrorCode) {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new ChangePasswordRequestDto(currentPassword, "new_password", "new_password");

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("currentPassword")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidPasswordProvider
      @DisplayName("Deve retornar 400 Bad Request quando a nova senha for inválida")
      void changePassword_shouldReturn400_whenNewPasswordIsInvalid(
          String newPassword, String expectedErrorCode) {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new ChangePasswordRequestDto(defaultPassword, newPassword, newPassword);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/password");
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

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("Deve retornar 400 Bad Request quando a senha de confirmação for nula ou vazia")
      void changePassword_shouldReturn400_whenConfirmPasswordIsInvalid(String confirmPassword) {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request =
            new ChangePasswordRequestDto(defaultPassword, "new_password", confirmPassword);

        // Act & Assert
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/password")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.CONFIRM_PASSWORD_REQUIRED;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/password");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("confirmPassword")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a senha atual estiver incorreta")
    void changePassword_shouldReturn400_whenCurrentPasswordIsIncorrect() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangePasswordRequestDto("wrong_password", "new_password", "new_password");

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .post("/password")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.PASSWORD_CURRENT_INCORRECT;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/users/me/password");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /api/users/me/phone/verification")
  class ChangePhoneTest {

    @Nested
    @DisplayName("Etapa 1: Iniciar alteração de telefone")
    class InitiateChangePhoneTest {
      @Test
      @DisplayName("Deve retornar 202 Accepted quando a alteração de telefone iniciar com sucesso")
      void initiateChangePhone_shouldReturn202_whenSuccessful() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new InitiateChangePhoneRequestDto("+5547992044567");

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .body(request)
            .when()
            .post("/phone/verification")
            .then()
            .statusCode(202);
      }

      @Nested
      @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
      class InvalidInputTests {

        @InvalidPhoneProvider
        @DisplayName("Deve retornar 400 Bad Request quando o número de telefone for inválido")
        void initiateChangePhone_shouldReturn400_whenPhoneIsEmptyOrNull(
            String invalidPhone, String expectedErrorCode) {
          // Arrange
          mockPersistUser();
          AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
          var request = new InitiateChangePhoneRequestDto(invalidPhone);

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", "Bearer " + tokens.accessToken())
                  .body(request)
                  .when()
                  .post("/phone/verification")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
          List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

          assertThat(response.status()).isEqualTo(400);
          assertThat(response.path()).isEqualTo("/api/users/me/phone/verification");
          assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
          assertThat(response.developerMessage())
              .isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

          assertThat(fieldErrors).isNotEmpty();
          assertThat(fieldErrors)
              .anyMatch(
                  fieldError ->
                      fieldError.fieldName().equals("newPhone")
                          && fieldError.errorCode().equals(errorCode.name())
                          && fieldError.developerMessage().equals(errorCode.getMessage()));
        }
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando o número de telefone for inválido")
      void initiateChangePhone_shouldReturn400_whenPhoneIsInvalid() {
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new InitiateChangePhoneRequestDto("+999999999999");

        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/phone/verification")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PHONE_INVALID_FORMAT;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/phone/verification");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando o número de telefone já está em uso")
      void initiateChangePhone_shouldReturn409_whenPhoneAlreadyExists() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        User mockExistingUser =
            mockPersistUser("existing_user", "Existing User", "+5521921340987", "123456");
        var request = new InitiateChangePhoneRequestDto(mockExistingUser.getPhone());

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/phone/verification")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PHONE_ALREADY_EXISTS;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/users/me/phone/verification");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Etapa 2: Completar alteração de telefone")
    class CompleteChangePhoneTest {

      @Test
      @DisplayName("Deve retornar 200 OK quando a verificação de telefone for um sucesso")
      void completeChangePhone_shouldReturn200_whenSuccessful() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        String newPhone = "+5547992044567";
        pendingPhoneChangePort.save(mockUser.getId(), newPhone);
        OtpCode verificationCode = otpPort.generateOtpCode(mockUser.getId());
        var request = new CompletePhoneChangeRequestDto(verificationCode);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .body(request)
            .when()
            .patch("/phone/verification/confirm")
            .then()
            .statusCode(200);

        User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
        assertThat(updatedUser.getPhone()).isEqualTo(newPhone);
      }

      @Nested
      @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
      class InvalidInputTests {

        @InvalidOtpCodeProvider
        @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido no DTO")
        void completeChangePhone_shouldReturn400_whenOtpCodeIsNullOrEmpty(
            String invalidOtp, String expectedErrorCode) {
          // Arrange
          User mockUser = mockPersistUser();
          AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
          String newPhone = "+5547992044567";
          pendingPhoneChangePort.save(mockUser.getId(), newPhone);

          Map<String, String> requestBody = new HashMap<>();
          requestBody.put("otpCode", invalidOtp);

          // Act & Assert
          ErrorResponseDto response =
              given()
                  .spec(specification)
                  .header("Authorization", "Bearer " + tokens.accessToken())
                  .body(requestBody)
                  .when()
                  .patch("/phone/verification/confirm")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
          List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

          assertThat(response.status()).isEqualTo(400);
          assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/confirm");
          assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
          assertThat(response.developerMessage())
              .isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

          assertThat(fieldErrors).isNotEmpty();
          assertThat(fieldErrors)
              .anyMatch(
                  fieldError ->
                      fieldError.fieldName().equals("otpCode")
                          && fieldError.errorCode().equals(errorCode.name())
                          && fieldError.developerMessage().equals(errorCode.getMessage()));
        }
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando o código de verificação for inválido")
      void completeChangePhone_shouldReturn400_whenCodeIsIncorrect() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        String newPhone = "+5547992044567";
        pendingPhoneChangePort.save(mockUser.getId(), newPhone);

        OtpCode otpCode = OtpCode.generate();
        var request = new CompletePhoneChangeRequestDto(otpCode);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .patch("/phone/verification/confirm")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OTP_CODE_INVALID_OR_EXPIRED;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/confirm");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName(
          "Deve retornar 404 Not Found quando a solicitação de alteração de telefone expirar")
      void completeChangePhone_shouldReturn404_whenRequestHasExpired() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        OtpCode otpCode = OtpCode.generate();
        var request = new CompletePhoneChangeRequestDto(otpCode);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .patch("/phone/verification/confirm")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PHONE_CHANGE_NOT_INITIATED;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/confirm");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Reenviar código OTP caso não recebido")
    class ResendChangePhoneOtpTest {

      @Test
      @DisplayName("Deve retornar 204 No Content quando o código OTP for reenviado com sucesso")
      void resendChangePhoneOtp_shouldReturn204_whenSuccessful() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        String newPhone = "+5547992044567";
        pendingPhoneChangePort.save(mockUser.getId(), newPhone);

        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .when()
            .post("/phone/verification/resend")
            .then()
            .statusCode(204);
      }

      @Test
      @DisplayName("Deve retornar 404 Not Found quando não houver alteração de telefone pendente")
      void resendChangePhoneOtp_shouldReturn404_whenNoPendingPhoneChange() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .when()
                .post("/phone/verification/resend")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.PHONE_CHANGE_NOT_INITIATED;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/resend");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Teste para o endpoint /api/users/me/disable")
    class DisableMyAccountTest {

      @Test
      @DisplayName("Deve retornar 204 No Content quando a conta for desativada com sucesso")
      void disableMyAccount_shouldReturn204_whenSuccessful() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .when()
            .post("/disable")
            .then()
            .statusCode(204);

        User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
        assertThat(updatedUser.isEnabled()).isFalse();
      }
    }
  }
}
