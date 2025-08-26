package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.*;
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
@DisplayName("Testes de Integração para AuthController")
public class AuthControllerTest extends TestIntegrationBaseConfig {

  @Autowired private UserRepositoryPort userRepository;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private OtpPort otpPort;

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    // Mantém a configuração base do RestAssured
    super.setUpRestAssured();

    // Configurações específicas para o AuthController
    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/auth")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @AfterEach
  void cleanDatabase() {
    // Limpa o banco de dados antes de cada teste
    userJpaRepository.deleteAll();
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/login")
  class LoginTests {

    @Test
    @DisplayName("Deve retornar 200 OK para login com credenciais válidas")
    void login_shouldReturn200ForValidLogin() {
      // Arrange
      createAndPersistUser(AccountStatus.ACTIVE);
      LoginRequestDto request = new LoginRequestDto("testUser", "123456");

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
      TokenResponseDto responseBody = response.as(TokenResponseDto.class);
      assertThat(responseBody).isNotNull();
      assertThat(responseBody.username()).isEqualTo("testUser");
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
    @DisplayName("Deve retornar 401 Unauthorized quando credenciais inválidas forem fornecidas")
    void login_shouldReturn401ForInvalidLogin() {
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
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(401);
      assertThat(response.message())
          .isEqualTo("Credenciais inválidas. Por favor, verifique seu usuário e senha.");
      assertThat(response.timestamp()).isNotNull();
      assertThat(response.path()).isEqualTo("/api/auth/login");
    }

    @Test
    @DisplayName(
        "Deve retornar 403 Forbidden quando um usuário PENDING_VERIFICATION tentar fazer login")
    void login_shouldReturn403ForUserWithPendingVerificationStatus() {
      // Arrange
      createAndPersistUser(AccountStatus.PENDING_VERIFICATION);
      LoginRequestDto request = new LoginRequestDto("testUser", "123456");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.message()).isEqualTo("Sua conta ainda não foi verificada.");
      assertThat(response.path()).isEqualTo("/api/auth/login");
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando um usuário LOCKED tentar fazer login")
    void login_shouldReturn403ForUserWithLockedStatus() {
      // Arrange
      createAndPersistUser(AccountStatus.LOCKED);
      LoginRequestDto request = new LoginRequestDto("testUser", "123456");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.message())
          .isEqualTo("Sua conta está bloqueada. Por favor, contate o suporte.");
      assertThat(response.path()).isEqualTo("/api/auth/login");
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando um usuário DISABLED tentar fazer login")
    void login_shouldReturn403ForUserWithDisabledStatus() {
      // Arrange
      createAndPersistUser(AccountStatus.DISABLED);
      LoginRequestDto request = new LoginRequestDto("testUser", "123456");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.message()).isEqualTo("Está conta não está ativa.");
      assertThat(response.path()).isEqualTo("/api/auth/login");
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/refresh-token")
  class RefreshTokenTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando um token válido for enviado")
    void refreshToken_shouldReturn200ForValidToken() {
      // Arrange
      createAndPersistUser(AccountStatus.ACTIVE);
      LoginRequestDto loginRequest = new LoginRequestDto("testUser", "123456");
      Cookie initialRefreshTokenCookie =
          given()
              .spec(specification)
              .body(loginRequest)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .detailedCookie("refreshToken");

      // Act
      Response response =
          given()
              .spec(specification)
              .cookie(initialRefreshTokenCookie)
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(200)
              .extract()
              .response();

      // Assert
      TokenResponseDto responseBody = response.as(TokenResponseDto.class);
      assertThat(responseBody).isNotNull();
      assertThat(responseBody.username()).isEqualTo("testUser");
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie).isNotNull();
      assertThat(refreshTokenCookie.getValue()).isNotBlank();
      assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
      assertThat(refreshTokenCookie.isSecured()).isTrue();
      assertThat(refreshTokenCookie.getPath()).isEqualTo("/api/auth");
      assertThat(refreshTokenCookie.getMaxAge()).isGreaterThan(0);

      assertThat(refreshTokenCookie.getValue()).isNotEqualTo(initialRefreshTokenCookie.getValue());
    }

    @Test
    @DisplayName("Deve retornar 400 BadRequest quando um token inválido for enviado")
    void refreshToken_shouldReturn400ForInvalidToken() {
      // Arrange
      Cookie invalidCookie = new Cookie.Builder("refreshToken", "token-que-nao-existe").build();

      // Act
      Response response =
          given()
              .spec(specification)
              .cookie(invalidCookie)
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(400)
              .extract()
              .response();

      // Assert
      ErrorResponseDto errorResponse = response.as(ErrorResponseDto.class);
      assertThat(errorResponse).isNotNull();
      assertThat(errorResponse.status()).isEqualTo(400);
      assertThat(errorResponse.message()).isEqualTo("Formato inválido para o refresh token.");
      assertThat(errorResponse.timestamp()).isNotNull();
      assertThat(errorResponse.path()).isEqualTo("/api/auth/refresh-token");
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/signup")
  class SignupTests {

    @Test
    @DisplayName("Deve retornar 201 created quando os dados do usuário forem válidos")
    void signup_shouldReturn201ForValidSignup() {
      // Arrange
      var signupRequest =
          new SignupRequestDto("newUser", "New User", "+5547998765432", "password", "password");

      // Act
      SignupResponseDto response =
          given()
              .spec(specification)
              .body(signupRequest)
              .when()
              .post("/signup")
              .then()
              .statusCode(201)
              .extract()
              .as(SignupResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION.getValue());
      assertThat(response.message())
          .isEqualTo(
              "Conta criada com sucesso. Um código de verificação foi enviado para o seu telefone.");
      assertThat(response.identifier()).isEqualTo(signupRequest.username());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando as senhas não coincidirem")
    void signup_shouldReturn400ForInvalidSignup() {
      // Arrange
      var invalidSignupRequest =
          new SignupRequestDto(
              "username_test",
              "New User",
              "+5547912345678",
              "password",
              "differentPassword" // Senhas não coincidem
              );

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(invalidSignupRequest)
              .when()
              .post("/signup")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.fieldErrors())
          .hasSize(1)
          .extracting("fieldName", "message")
          .containsExactly(
              tuple("confirmPassword", "A senha de confirmação não corresponde à senha."));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone informado tem formato inválido")
    void signup_shouldReturn400ForPhoneNumberInvalidFormat() {
      // Arrange
      var invalidPhoneSignupRequest =
          new SignupRequestDto("test_user", "Test User 2", "+554712345678", "password", "password");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(invalidPhoneSignupRequest)
              .when()
              .post("/signup")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      assertThat(response.path()).isEqualTo("/api/auth/signup");
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando o username ou telefone já estão em uso")
    void signup_shouldReturn409ForDuplicateUsernameOrPhone() {
      // Arrange
      createAndPersistUser(AccountStatus.ACTIVE);
      var duplicateSignupRequest =
          new SignupRequestDto(
              "testUser", // Username já existente
              "Another User",
              "+5547912345678", // Telefone já existente
              "password",
              "password");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(duplicateSignupRequest)
              .when()
              .post("/signup")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Esse nome de usuário já está em uso.");
      assertThat(response.path()).isEqualTo("/api/auth/signup");
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/verify-account")
  class VerifyAccountTests {

    @Test
    @DisplayName("Deve retornar 200 OK e tokens quando o OTP for válido para um usuário pendente")
    void verifyAccount_shouldReturn200AndTokens_whenOtpIsValidForPendingUser() {
      // Arrange
      createAndPersistUser(AccountStatus.PENDING_VERIFICATION);
      User user = userRepository.findByUsername("testUser").orElseThrow();

      String validOtp = otpPort.generateAndSaveOtp(user.getId());
      var request = new VerifyAccountRequestDto("testUser", validOtp);

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

      // Assert
      TokenResponseDto responseBody = response.as(TokenResponseDto.class);
      assertThat(responseBody).isNotNull();
      assertThat(responseBody.username()).isEqualTo("testUser");
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie).isNotNull();

      User updatedUser = userRepository.findById(user.getId()).orElseThrow();
      assertThat(updatedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não existir")
    void verifyAccount_shouldReturn404_whenUserNotFound() {
      // Arrange
      var request = new VerifyAccountRequestDto("nonexistentUser", "123456");

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
              "Usuário não encontrado para realizar verificação. Por favor faça o cadastro novamente.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o OTP for inválido")
    void verifyAccount_shouldReturn400_whenOtpIsInvalid() {
      // Arrange
      createAndPersistUser(AccountStatus.PENDING_VERIFICATION);
      // OTP "111222" é deliberadamente incorreto
      var request = new VerifyAccountRequestDto("testUser", "111222");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/verify-account")
              .then()
              .statusCode(400) // Esperamos Bad Request conforme a hierarquia de exceções
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("Código de verificação inválido ou expirado.");
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar verificar uma conta que já está ativa")
    void verifyAccount_shouldReturn409_whenAccountIsAlreadyActive() {
      // Arrange
      createAndPersistUser(AccountStatus.ACTIVE);
      User user = userRepository.findByUsername("testUser").orElseThrow();

      String validOtp = otpPort.generateAndSaveOtp(user.getId());
      var request = new VerifyAccountRequestDto("testUser", validOtp);

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
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/resend-code")
  class ResendCodeTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando o código for reenviado com sucesso")
    void resendCode_shouldReturn200_whenSuccessful() {
      // Arrange
      createAndPersistUser(AccountStatus.PENDING_VERIFICATION);
      ResendCodeRequestDto request = new ResendCodeRequestDto("testUser");

      // Act
      ResendCodeResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(200)
              .extract()
              .as(ResendCodeResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.message()).isEqualTo("Código de verificação reenviado com sucesso.");
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não existir")
    void resendCode_shouldReturn404_whenUserNotFound() {
      // Arrange
      ResendCodeRequestDto request = new ResendCodeRequestDto("nonexistentUser");

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/resend-code")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.message())
          .isEqualTo(
              "Usuário não encontrado para reenviar o código. Por favor realize o cadastro novamente.");
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar reenviar código para uma conta já ativa")
    void resendCode_shouldReturn409_whenAccountIsAlreadyActive() {
      // Arrange
      createAndPersistUser(AccountStatus.ACTIVE);
      var request = new ResendCodeRequestDto("testUser");

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
    }
  }

  private void createAndPersistUser(AccountStatus status) {
    String passwordEncoded = passwordEncoder.encode("123456");
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            "testUser",
            "Test User",
            "+5547912345678",
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            Instant.now());

    userRepository.save(user);
  }
}
