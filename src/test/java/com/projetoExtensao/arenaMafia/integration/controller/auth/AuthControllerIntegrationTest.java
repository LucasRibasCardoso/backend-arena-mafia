package com.projetoExtensao.arenaMafia.integration.controller.auth;

import static com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus.LOCKED;
import static com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus.PENDING_VERIFICATION;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.AuthResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.*;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AuthController")
public class AuthControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private OtpPort otpPort;
  @Autowired private OtpSessionPort otpSessionPort;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;

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
  @DisplayName("Testes para o endpoint /auth/login")
  class LoginTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o usuário for autenticado com sucesso")
    void login_shouldReturn200_whenUserAuthenticateSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

      // Act
      Response response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .response();

      // Assert
      AuthResponseDto responseBody = response.as(AuthResponseDto.class);
      assertThat(responseBody.phone()).isEqualTo(mockUser.getPhone());
      assertThat(responseBody.username()).isEqualTo(mockUser.getUsername());
      assertThat(responseBody.fullName()).isEqualTo(mockUser.getFullName());
      assertThat(responseBody.role()).isEqualTo(mockUser.getRole().name());
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie).isNotNull();
      assertThat(refreshTokenCookie.getValue()).isNotBlank();
      assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
      assertThat(refreshTokenCookie.isSecured()).isTrue();
      assertThat(refreshTokenCookie.getPath()).isEqualTo("/api/auth");
      assertThat(refreshTokenCookie.getMaxAge()).isGreaterThan(0);
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidUsernameProvider
      @DisplayName("Deve retornar 400 Bad Request para username inválido")
      void login_shouldReturn400_whenUsernameIsInvalid(String username, String expectedErrorCode) {
        // Arrange
        LoginRequestDto request = new LoginRequestDto(username, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/login");
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

      @InvalidPasswordProvider
      @DisplayName("Deve retornar 400 Bad Request para password inválido")
      void login_shouldReturn400_whenPasswordIsInvalid(String password, String expectedErrorCode) {
        // Arrange
        LoginRequestDto request = new LoginRequestDto(defaultUsername, password);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("password")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando as credenciais forem inválidas")
    void login_shouldReturn401_whenCredentialsAreInvalid() {
      // Arrange
      LoginRequestDto request = new LoginRequestDto("invaliduser", "wrongpassword");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.INVALID_CREDENTIALS;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/auth/login");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Nested
    @DisplayName("Deve retornar 401 Unauthorized quando a conta não está ativada")
    class AccountStateTests {
      @ParameterizedTest
      @EnumSource(
          value = AccountStatus.class,
          names = {"DISABLED", "LOCKED", "PENDING_VERIFICATION"})
      void login_shouldReturn401_whenAccountNotActive(AccountStatus invalidStatus) {
        // Arrange
        mockPersistUser(invalidStatus);
        LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

        ErrorCode expectedErrorCode =
            switch (invalidStatus) {
              case DISABLED -> ErrorCode.ACCOUNT_DISABLED;
              case LOCKED -> ErrorCode.ACCOUNT_LOCKED;
              case PENDING_VERIFICATION -> ErrorCode.ACCOUNT_PENDING_VERIFICATION;
              default -> throw new IllegalStateException("Status inesperado: " + invalidStatus);
            };

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(401)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
        assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 401 Unauthorized quando a conta está bloqueada")
      void login_shouldReturn401_whenAccountIsLocked() {
        // Arrange
        mockPersistUser(LOCKED);
        LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(401)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_LOCKED;

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 401 Unauthorized quando a conta está desativada")
      void login_shouldReturn401_whenAccountIsDisabled() {
        // Arrange
        mockPersistUser(AccountStatus.DISABLED);
        LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(401)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.ACCOUNT_DISABLED;

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/logout")
  class LogoutTests {

    @Test
    @DisplayName("Deve retornar 204 No Content quando o logout for realizado com sucesso")
    void logout_shouldReturn204_whenLogoutIsSuccessfully() {
      // Arrange
      mockPersistUser(AccountStatus.ACTIVE);
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

      // Act
      Response response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .cookie(tokens.refreshTokenCookie())
              .when()
              .post("/logout")
              .then()
              .statusCode(204)
              .extract()
              .response();

      // Verifica se o servidor instruiu o navegador a apagar o cookie.
      Cookie expiredCookie = response.getDetailedCookie("refreshToken");
      assertThat(expiredCookie).isNotNull();
      assertThat(expiredCookie.getMaxAge()).isEqualTo(0);

      assertThat(refreshTokenRepository.findByToken(tokens.refreshToken())).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o refresh token for inválido")
    void logout_shouldReturn400_whenRefreshTokenIsInvalid() {
      // Arrange
      mockPersistUser(AccountStatus.ACTIVE);
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

      Cookie invalidRefreshToken = new Cookie.Builder("refreshToken", "invalid-token").build();

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .cookie(invalidRefreshToken)
              .when()
              .post("/logout")
              .then()
              .log()
              .all()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.REFRESH_TOKEN_INVALID_FORMAT;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/logout");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized ao tentar fazer logout sem autenticação")
    void logout_shouldReturn401_whenNotAuthenticated() {
      // Act & Assert
      ErrorResponseDto response =
          given()
              .spec(specification)
              .when()
              .post("/logout")
              .then()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.SESSION_EXPIRED;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/auth/logout");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/refresh-token")
  class RefreshTokenTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o token for atualizado com sucesso")
    void refreshToken_shouldReturn200_whenTokenUpdatedSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

      // Act
      Response response =
          given()
              .spec(specification)
              .cookie(tokens.refreshTokenCookie())
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(200)
              .extract()
              .response();

      // Assert
      AuthResponseDto responseBody = response.as(AuthResponseDto.class);
      assertThat(responseBody.phone()).isEqualTo(mockUser.getPhone());
      assertThat(responseBody.username()).isEqualTo(mockUser.getUsername());
      assertThat(responseBody.fullName()).isEqualTo(mockUser.getFullName());
      assertThat(responseBody.role()).isEqualTo(mockUser.getRole().name());
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie.getValue()).hasSize(36); // UUID tem 36 caracteres
      assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
      assertThat(refreshTokenCookie.isSecured()).isTrue();
      assertThat(refreshTokenCookie.getPath()).isEqualTo("/api/auth");
      assertThat(refreshTokenCookie.getMaxAge()).isGreaterThan(0);

      assertThat(refreshTokenCookie.getValue()).isNotEqualTo(tokens.refreshToken().toString());
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @Test
      @DisplayName("Deve retornar 400 BadRequest quando o refresh token não for enviado")
      void refreshToken_shouldReturn400_whenRefreshTokenIsNullOrEmpty() {
        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .when()
                .post("/refresh-token")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.REFRESH_TOKEN_REQUIRED;
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @InvalidRefreshTokenProvider
      @DisplayName("Deve retornar 400 BadRequest quando um refresh token for inválido")
      void refreshToken_shouldReturn400_whenRefreshTokenIsInvalid(
          String refreshToken, String expectedErrorCode) {
        // Arrange
        Cookie invalidCookie = new Cookie.Builder("refreshToken", refreshToken).build();

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .cookie(invalidCookie)
                .when()
                .post("/refresh-token")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando o refresh token não for encontrado")
    void refreshToken_shouldReturn401_whenTokenNotFound() {
      // Arrange
      String refreshTokenVO = RefreshTokenVO.generate().toString();
      Cookie nonExistentTokenCookie = new Cookie.Builder("refreshToken", refreshTokenVO).build();

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .cookie(nonExistentTokenCookie)
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.REFRESH_TOKEN_NOT_FOUND;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando o refresh token estiver expirado")
    void refreshToken_shouldReturn401_whenTokenExpired() {
      // Arrange
      User user = mockPersistUser(AccountStatus.ACTIVE);
      RefreshToken expiredToken = mockPersistRefreshToken(-1L, user);

      Cookie expiredTokenCookie =
          new Cookie.Builder("refreshToken", expiredToken.getToken().toString()).build();

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .cookie(expiredTokenCookie)
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.REFRESH_TOKEN_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Nested
    @DisplayName("Deve retornar 403 Forbidden quando a conta não está ativada")
    class AccountStateTests {

      @ParameterizedTest
      @EnumSource(
          value = AccountStatus.class,
          names = {"DISABLED", "LOCKED", "PENDING_VERIFICATION"})
      void refreshToken_shouldReturn403_whenAccountNotActive(AccountStatus invalidStatus) {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        alterAccountStatus(mockUser.getId(), invalidStatus);

        ErrorCode expectedErrorCode =
            switch (invalidStatus) {
              case DISABLED -> ErrorCode.ACCOUNT_DISABLED;
              case LOCKED -> ErrorCode.ACCOUNT_LOCKED;
              case PENDING_VERIFICATION -> ErrorCode.ACCOUNT_PENDING_VERIFICATION;
              default -> throw new IllegalStateException("Status inesperado: " + invalidStatus);
            };

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .cookie(tokens.refreshTokenCookie())
                .when()
                .post("/refresh-token")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
        assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
        assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/signup")
  class SignupTests {

    @Test
    @DisplayName("Deve retornar 202 Accept quando o usuário for criado com sucesso")
    void signup_shouldReturn202_whenUserDataIsValid() {
      // Arrange
      var request =
          new SignupRequestDto(
              defaultUsername, defaultFullName, defaultPhone, defaultPassword, defaultPassword);

      // Act
      SignupResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/signup")
              .then()
              .statusCode(202)
              .extract()
              .as(SignupResponseDto.class);

      // Assert
      assertThat(response.otpSessionId().toString()).hasSize(36); // UUID
      assertThat(response.message())
          .isEqualTo(
              "Conta criada com sucesso. Um código de verificação foi enviado para o seu telefone.");
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando as senhas não coincidirem")
      void signup_shouldReturn400_whenPasswordsNotCoincide() {
        // Arrange
        var request =
            new SignupRequestDto(
                defaultUsername,
                defaultFullName,
                defaultPhone,
                defaultPassword,
                "differentPassword");

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.PASSWORDS_DO_NOT_MATCH;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
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

      @InvalidUsernameProvider
      @DisplayName("Deve retornar 400 Bad Request quando o username for inválido")
      void signup_shouldReturn400_whenUsernameIsInvalid(String username, String expectedErrorCode) {
        // Arrange
        var request =
            new SignupRequestDto(
                username, defaultFullName, defaultPhone, defaultPassword, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
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

      @InvalidFullNameProvider
      @DisplayName("Deve retornar 400 Bad Request quando o nome completo for inválido")
      void signup_shouldReturn400_whenFullNameIsInvalid(String fullName, String expectedErrorCode) {
        // Arrange
        var request =
            new SignupRequestDto(
                defaultUsername, fullName, defaultPhone, defaultPassword, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
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

      @InvalidPhoneProvider
      @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido")
      void signup_shouldReturn400_whenPhoneNumberIsInvalid(String phone, String expectedErrorCode) {
        // Arrange
        var request =
            new SignupRequestDto(
                defaultUsername, defaultFullName, phone, defaultPassword, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("phone")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidPasswordProvider
      @DisplayName("Deve retornar 400 Bad Request quando a senha for inválida")
      void signup_shouldReturn400_whenPasswordIsInvalid(String password, String expectedErrorCode) {
        // Arrange
        var request =
            new SignupRequestDto(
                defaultUsername, defaultFullName, defaultPhone, password, password);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("password")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("Deve retornar 400 Bad Request quando a senha de confirmação for nula ou vazia")
      void signup_shouldReturn400_whenConfirmPasswordIsNullOrEmpty(String confirmPassword) {
        // Arrange
        var request =
            new SignupRequestDto(
                defaultUsername, defaultFullName, defaultPhone, defaultPassword, confirmPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.CONFIRM_PASSWORD_REQUIRED;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
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
    @DisplayName("Deve retornar 400 Bad Request quando o número de telefone for inválido")
    void signup_shouldReturn400_whenPhoneNumberIsInvalidFormat() {
      // Arrange
      String invalidPhone = "+999123456789"; // Código de país inválido
      var request =
          new SignupRequestDto(
              defaultUsername, defaultFullName, invalidPhone, defaultPassword, defaultPassword);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/signup")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.PHONE_INVALID_FORMAT;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/signup");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando os dados já estão em uso")
    class UserAlreadyExistsTests {
      @Test
      @DisplayName("Deve retornar 409 Conflict quando o username já está em uso")
      void signup_shouldReturn409_whenUsernameAlreadyExists() {
        // Arrange
        User mockUser = mockPersistUser();
        String newPhone = "+5583998765432";
        var request =
            new SignupRequestDto(
                mockUser.getUsername(),
                defaultFullName,
                newPhone,
                defaultPassword,
                defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.USERNAME_ALREADY_EXISTS;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando o telefone já está em uso")
      void signup_shouldReturn409_whenPhoneAlreadyExists() {
        // Arrange
        User mockUser = mockPersistUser();
        String newUsername = "new_user";
        var request =
            new SignupRequestDto(
                newUsername,
                defaultFullName,
                mockUser.getPhone(),
                defaultPassword,
                defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/signup")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.PHONE_ALREADY_EXISTS;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/auth/signup");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/verify-account")
  class VerifyAccountTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando a conta for ativada com sucesso")
    void verifyAccount_shouldReturn200_whenActiveAccountSuccessful() {
      // Arrange
      User mockUser = mockPersistUser(PENDING_VERIFICATION);
      OtpCode otpCode = otpPort.generateOtpCode(mockUser.getId());
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

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
      AuthResponseDto responseBody = response.as(AuthResponseDto.class);

      // Assert
      assertThat(responseBody.userId()).isEqualTo(mockUser.getId().toString());
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

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidOtpSessionIdProvider
      @DisplayName("Deve retornar 400 Bad Request quando o otpSessionId for inválido")
      void verifyAccount_shouldReturn400_whenOtpSessionIdIsInvalid(
          String otpSessionId, String expectedErrorCode) {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("otpSessionId", otpSessionId);
        requestBody.put("otpCode", "123456");

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(requestBody)
                .when()
                .post("/verify-account")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/verify-account");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("otpSessionId")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidOtpCodeProvider
      @DisplayName("Deve retornar 400 Bad Request quando o otpCode for inválido")
      void verifyAccount_shouldReturn400_whenOtpCodeIsInvalid(
          String otpCode, String expectedErrorCode) {
        // Arrange
        String otpSessionId = OtpSessionId.generate().toString();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("otpSessionId", otpSessionId);
        requestBody.put("otpCode", otpCode);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(requestBody)
                .when()
                .post("/verify-account")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/verify-account");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

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
    @DisplayName("Deve retornar 400 Bad Request quando a sessão OTP for inválida ou expirada")
    void validateResetToken_shouldReturn400_whenUserIdIsInvalid() {
      // Arrange
      OtpSessionId invalidOtpSessionId = OtpSessionId.generate();
      OtpCode otpCode = OtpCode.generate();
      var request = new ValidateOtpRequestDto(invalidOtpSessionId, otpCode);

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
      ErrorCode errorCode = ErrorCode.OTP_SESSION_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido ou expirado")
    void verifyAccount_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      User mockUser = mockPersistUser();
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());
      OtpCode invalidCodeOTP = OtpCode.generate();

      var request = new ValidateOtpRequestDto(otpSessionId, invalidCodeOTP);

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
      ErrorCode errorCode = ErrorCode.OTP_CODE_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void verifyAccount_shouldReturn404_whenUserNotFound() {
      // Arrange
      UUID userId = UUID.randomUUID();
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(userId);
      OtpCode otpCode = OtpCode.generate();
      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

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
      ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden ao tentar ativar uma conta que já está ativa")
    void verifyAccount_shouldReturn403_whenAccountIsAlreadyActive() {
      // Arrange
      User mockUser = mockPersistUser(AccountStatus.ACTIVE);
      OtpCode otpCode = otpPort.generateOtpCode(mockUser.getId());
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.ACCOUNT_NOT_PENDING_VERIFICATION;

      assertThat(response.status()).isEqualTo(403);
      assertThat(response.path()).isEqualTo("/api/auth/verify-account");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/resend-otp")
  class ResendOtpTests {

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "PENDING_VERIFICATION"})
    @DisplayName(
        "Deve retornar 204 No Content quando o OTP for reenviado com sucesso para conta ativa ou pendente")
    void resendOtp_shouldReturn204_whenOtpIsResentSuccessfullyToActiveOrPendingAccount(
        AccountStatus status) {
      // Arrange
      User mockUser = mockPersistUser(status);
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());

      var request = new ResendOtpRequestDto(otpSessionId);

      // Act & Assert
      given().spec(specification).body(request).when().post("/resend-otp").then().statusCode(204);
    }

    @Nested
    @DisplayName("Deve retornar 400 Bad Request quando os dados de entrada forem inválidos")
    class InvalidInputTests {

      @InvalidOtpSessionIdProvider
      @DisplayName("Deve retornar 400 Bad Request quando o otpSessionId for inválido")
      void resendOtp_shouldReturn400_whenOtpSessionIdIsInvalid(
          String otpSessionId, String expectedErrorCode) {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("otpSessionId", otpSessionId);
        requestBody.put("otpCode", "123456");

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(requestBody)
                .when()
                .post("/resend-otp")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/auth/resend-otp");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("otpSessionId")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a sessão for inválida ou expirada")
    void resendOtp_shouldReturn400_whenOtpSessionIsInvalid() {
      // Arrange
      OtpSessionId otpSessionId = OtpSessionId.generate();
      var request = new ResendOtpRequestDto(otpSessionId);

      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-otp")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.OTP_SESSION_INVALID_OR_EXPIRED;

      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/resend-otp");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void resendOtp_shouldReturn404_whenUserNotFound() {
      // Arrange
      UUID userId = UUID.randomUUID();
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(userId);
      var request = new ResendOtpRequestDto(otpSessionId);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-otp")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/auth/resend-otp");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Nested
    @DisplayName("Deve retornar 403 Forbidden quando a conta não está ativa ou pendente")
    class AccountStateConflictTests {

      @ParameterizedTest
      @EnumSource(
          value = AccountStatus.class,
          names = {"DISABLED", "LOCKED"})
      @DisplayName("Deve lançar 403 Forbidden quando a conta está desativada ou bloqueada")
      void resendOtp_shouldReturn403_whenAccountIsDisabledOrLocked(AccountStatus invalidStatus) {
        // Arrange
        User mockUser = mockPersistUser(invalidStatus);
        OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());
        var request = new ResendOtpRequestDto(otpSessionId);

        ErrorCode expectedErrorCode =
            switch (invalidStatus) {
              case DISABLED -> ErrorCode.ACCOUNT_DISABLED;
              case LOCKED -> ErrorCode.ACCOUNT_LOCKED;
              default -> throw new IllegalStateException("Status inesperado: " + invalidStatus);
            };

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/resend-otp")
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/auth/resend-otp");
        assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
        assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
      }
    }
  }
}
