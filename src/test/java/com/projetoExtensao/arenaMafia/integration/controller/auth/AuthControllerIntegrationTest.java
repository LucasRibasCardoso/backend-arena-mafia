package com.projetoExtensao.arenaMafia.integration.controller.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
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
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o username for inválido no DTO")
    void login_shouldReturn400_whenUsernameIsBlank() {
      // Arrange
      String blankUsername = "   ";
      LoginRequestDto request = new LoginRequestDto(blankUsername, defaultPassword);

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
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.path()).isEqualTo("/api/auth/login");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("username");
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando as credenciais forem inválidas")
    void login_shouldReturn401_whenCredentialsAreInvalid() {
      // Arrange
      String invalidUsername = "invaliduser";
      String invalidPassword = "wrongpassword";
      LoginRequestDto request = new LoginRequestDto(invalidUsername, invalidPassword);

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
      assertThat(response.message())
          .isEqualTo("Credenciais inválidas. Por favor, verifique seu usuário e senha.");
      assertThat(response.path()).isEqualTo("/api/auth/login");
      assertThat(response.fieldErrors()).isNull();
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando a conta não está ativada")
    class AccountStateTests {
      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está pendente de verificação")
      void login_shouldReturn409_whenAccountIsPendingVerification() {
        // Arrange
        mockPersistUser(AccountStatus.PENDING_VERIFICATION);
        LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.fieldErrors()).isNull();
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está bloqueada")
      void login_shouldReturn409_whenAccountIsLocked() {
        // Arrange
        mockPersistUser(AccountStatus.LOCKED);
        LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/login")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.message())
            .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.fieldErrors()).isNull();
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está desativada")
      void login_shouldReturn409_whenAccountIsDisabled() {
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
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");
        assertThat(response.path()).isEqualTo("/api/auth/login");
        assertThat(response.fieldErrors()).isNull();
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

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.message())
          .isEqualTo("Sua sessão é inválida ou expirou. Por favor, faça login novamente.");
      assertThat(response.path()).isEqualTo("/api/auth/logout");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o refresh token for inválido")
    void logout_shouldReturn400_whenRefreshTokenIsInvalid() {
      // Arrange
      mockPersistUser(AccountStatus.ACTIVE);
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

      Cookie invalidRefreshToken =
          new Cookie.Builder("refreshToken", "invalid-refresh-token-format").build();

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .cookie(invalidRefreshToken)
              .when()
              .post("/logout")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.message()).isEqualTo("Formato inválido para o refresh token.");
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

    @Test
    @DisplayName("Deve retornar 400 BadRequest quando um refresh token for inválido")
    void refreshToken_shouldReturn400_whenRefreshTokenIsInvalid() {
      // Arrange
      Cookie invalidCookie =
          new Cookie.Builder("refreshToken", "invalid-refresh-token-format").build();

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
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("Formato inválido para o refresh token.");
      assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando o refresh token não for enviado")
    void refreshToken_shouldReturn401_whenTokenNotProvided() {
      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(401);
      assertThat(response.message())
          .isEqualTo("Sua sessão expirou. Por favor, faça login novamente.");
      assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando o refresh token não for encontrado")
    void refreshToken_shouldReturn401_whenTokenNotFound() {
      // Arrange
      RefreshTokenVO refreshTokenVO = RefreshTokenVO.generate();
      Cookie nonExistentTokenCookie =
          new Cookie.Builder("refreshToken", refreshTokenVO.toString()).build();

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
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(401);
      assertThat(response.message()).isEqualTo("Refresh token não encontrado.");
      assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
      assertThat(response.fieldErrors()).isNull();
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando a conta não está ativada")
    class AccountStateTests {
      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está bloqueada")
      void refreshToken_shouldReturn409_whenUserAccountIsLocked() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        alterAccountStatus(mockUser.getId(), AccountStatus.LOCKED);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .cookie(tokens.refreshTokenCookie())
                .when()
                .post("/refresh-token")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.message())
            .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
        assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
        assertThat(response.fieldErrors()).isNull();
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está pendente de verificação")
      void refreshToken_shouldReturn409_whenUserAccountIsPendingVerification() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        alterAccountStatus(mockUser.getId(), AccountStatus.PENDING_VERIFICATION);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .cookie(tokens.refreshTokenCookie())
                .when()
                .post("/refresh-token")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
        assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
        assertThat(response.fieldErrors()).isNull();
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta está desativada")
      void refreshToken_shouldReturn409_whenUserAccountIsDisabled() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        alterAccountStatus(mockUser.getId(), AccountStatus.DISABLED);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .cookie(tokens.refreshTokenCookie())
                .when()
                .post("/refresh-token")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");
        assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
        assertThat(response.fieldErrors()).isNull();
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

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando as senhas não coincidirem")
    void signup_shouldReturn400_whenPasswordsNotCoincide() {
      // Arrange
      var request =
          new SignupRequestDto(
              defaultUsername, defaultFullName, defaultPhone, defaultPassword, "differentPassword");

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
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("confirmPassword");
      assertThat(response.fieldErrors().getFirst().message())
          .isEqualTo("A senha de confirmação não corresponde à senha.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone for inválido")
    void signup_shouldReturn400_whenPhoneNumberIsInvalid() {
      // Arrange
      String invalidPhone = "+999123456789";
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
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando o usuário já existe")
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
        assertThat(response.message()).isEqualTo("Esse nome de usuário já está em uso.");
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
        assertThat(response.message()).isEqualTo("Esse número de telefone já está em uso.");
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/verify-account")
  class VerifyAccountTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código OTP for válido e a conta estiver pendente")
    void verifyAccount_shouldReturn200_whenOtpIsValidForPendingUser() {
      // Arrange
      User mockUser = mockPersistUser(AccountStatus.PENDING_VERIFICATION);
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

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o código OTP for inválido")
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
      assertThat(response.message()).isEqualTo("Código de verificação inválido ou expirado.");
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
      assertThat(response.message()).isEqualTo("Sessão de verificação inválida ou expirada.");
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
      assertThat(response.message())
          .isEqualTo(
              "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta.");
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar ativar uma conta que já está ativa")
    void verifyAccount_shouldReturn409_whenAccountIsAlreadyActive() {
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
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.message())
          .isEqualTo("Não é possível ativar uma conta que não está pendente de verificação.");
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
      assertThat(response.message()).isEqualTo("Sessão de verificação inválida ou expirada.");
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
      assertThat(response.message()).isEqualTo("Usuário não encontrado.");
    }

    @Nested
    @DisplayName("Deve retornar 409 Conflict quando a conta não está ativa ou pendente")
    class AccountStateConflictTests {

      @ParameterizedTest
      @EnumSource(
          value = AccountStatus.class,
          names = {"DISABLED", "LOCKED"})
      @DisplayName("Deve lançar exceção quando a conta está desativada ou bloqueada")
      void resendOtp_shouldReturn409_whenAccountIsDisabledOrLocked(AccountStatus status) {
        // Arrange
        User mockUser = mockPersistUser(status);
        OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(mockUser.getId());
        var request = new ResendOtpRequestDto(otpSessionId);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .body(request)
                .when()
                .post("/resend-otp")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo(
                "Atenção: Sua conta está bloqueada ou desativada. Por favor, contate o suporte.");
      }
    }
  }
}
