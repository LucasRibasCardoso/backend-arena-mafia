package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
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
@DisplayName("Testes de Integração para AuthController")
public class AuthControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private RequestSpecification specification;

  private final String defaultPhone = "+558320548186";
  private final String defaultUsername = "test_user";
  private final String defaultFullName = "Test User";
  private final String defaultPassword = "123456";
  private final AccountStatus defaultStatus = AccountStatus.ACTIVE;
  private final RoleEnum defaultRole = RoleEnum.ROLE_USER;

  private final String defaultConfirmPassword = defaultPassword;

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

  private void alterAccountStatus(AccountStatus status) {
    User user = userRepository.findByPhone(defaultPhone).orElseThrow();
    User lockedUser =
        User.reconstitute(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getPhone(),
            user.getPasswordHash(),
            status,
            user.getRole(),
            user.getCreatedAt());
    userRepository.save(lockedUser);
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/login")
  class LoginTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando as credenciais forem válidas")
    void login_shouldReturn200_whenCredentialsAreValid() {
      // Arrange
      mockPersistUser(defaultStatus);
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
      TokenResponseDto responseBody = response.as(TokenResponseDto.class);
      assertThat(responseBody.phone()).isEqualTo(defaultPhone);
      assertThat(responseBody.username()).isEqualTo(defaultUsername);
      assertThat(responseBody.fullName()).isEqualTo(defaultFullName);
      assertThat(responseBody.role()).isEqualTo(defaultRole.name());
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
    @DisplayName("Deve retornar 400 Bad Request quando o username estiver em branco")
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

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta estiver pendente de verificação")
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
    @DisplayName("Deve retornar 409 Conflict quando a conta estiver bloqueada")
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
    @DisplayName("Deve retornar 409 Conflict quando a conta estiver desativada")
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
      assertThat(response.message()).isEqualTo("Atenção: Sua conta está desativada.");
      assertThat(response.path()).isEqualTo("/api/auth/login");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/logout")
  class LogoutTests {

    @Test
    @DisplayName("Deve retornar 204 No Content quando o logout for realizado com sucesso")
    void logout_shouldReturn204_whenLogoutIsSuccessfully() {
      // Arrange
      mockPersistUser(defaultStatus);
      LoginRequestDto loginRequest = new LoginRequestDto(defaultUsername, defaultPassword);

      Response loginResponse =
          given()
              .spec(specification)
              .body(loginRequest)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .response();

      String accessToken = loginResponse.as(TokenResponseDto.class).accessToken();
      Cookie refreshTokenCookie = loginResponse.getDetailedCookie("refreshToken");
      RefreshTokenVO refreshToken = RefreshTokenVO.fromString(refreshTokenCookie.getValue());

      // Garante que o refresh token existe no banco de dados ANTES do logout.
      assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();

      // Act
      Response response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + accessToken)
              .cookie(refreshTokenCookie)
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

      assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
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
      mockPersistUser(defaultStatus);
      LoginRequestDto loginRequest = new LoginRequestDto(defaultUsername, defaultPassword);

      Response loginResponse =
          given()
              .spec(specification)
              .body(loginRequest)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .response();

      String accessToken = loginResponse.as(TokenResponseDto.class).accessToken();
      Cookie invalidRefreshTokenCookie =
          new Cookie.Builder("refreshToken", "invalid-token-format").build();

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + accessToken)
              .cookie(invalidRefreshTokenCookie)
              .when()
              .post("/logout")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message()).isEqualTo("Formato inválido para o refresh token.");
      assertThat(response.path()).isEqualTo("/api/auth/logout");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/refresh-token")
  class RefreshTokenTests {

    @Test
    @DisplayName("Deve retornar 200 OK quando um refresh token válido for enviado")
    void refreshToken_shouldReturn200_whenTokenIsValid() {
      // Arrange
      mockPersistUser(defaultStatus);
      LoginRequestDto loginRequest = new LoginRequestDto(defaultUsername, defaultPassword);
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
      assertThat(responseBody.phone()).isEqualTo(defaultPhone);
      assertThat(responseBody.username()).isEqualTo(defaultUsername);
      assertThat(responseBody.fullName()).isEqualTo(defaultFullName);
      assertThat(responseBody.role()).isEqualTo(defaultRole.name());
      assertThat(responseBody.accessToken()).isNotBlank();

      Cookie refreshTokenCookie = response.getDetailedCookie("refreshToken");
      assertThat(refreshTokenCookie.getValue()).hasSize(36); // UUID tem 36 caracteres
      assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
      assertThat(refreshTokenCookie.isSecured()).isTrue();
      assertThat(refreshTokenCookie.getPath()).isEqualTo("/api/auth");
      assertThat(refreshTokenCookie.getMaxAge()).isGreaterThan(0);

      assertThat(refreshTokenCookie.getValue()).isNotEqualTo(initialRefreshTokenCookie.getValue());
    }

    @Test
    @DisplayName("Deve retornar 400 BadRequest quando um refresh token inválido for enviado")
    void refreshToken_shouldReturn400ForInvalidToken() {
      // Arrange
      Cookie invalidCookie = new Cookie.Builder("refreshToken", "token-que-nao-existe").build();

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
      Cookie nonExistentTokenCookie =
          new Cookie.Builder("refreshToken", UUID.randomUUID().toString()).build();

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

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver bloqueada")
    void refreshToken_shouldReturn409_whenUserAccountIsLocked() {
      // Arrange
      mockPersistUser(defaultStatus);
      LoginRequestDto loginRequest = new LoginRequestDto(defaultUsername, defaultPassword);
      Cookie refreshTokenCookie =
          given()
              .spec(specification)
              .body(loginRequest)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .detailedCookie("refreshToken");

      alterAccountStatus(AccountStatus.LOCKED);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .cookie(refreshTokenCookie)
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
    @DisplayName(
        "Deve retornar 409 Conflict quando a conta do usuário estiver pendente de verificação")
    void refreshToken_shouldReturn409_whenUserAccountIsPendingVerification() {
      // Arrange
      mockPersistUser(defaultStatus);
      LoginRequestDto loginRequest = new LoginRequestDto(defaultUsername, defaultPassword);
      Cookie refreshTokenCookie =
          given()
              .spec(specification)
              .body(loginRequest)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .detailedCookie("refreshToken");

      alterAccountStatus(AccountStatus.PENDING_VERIFICATION);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .cookie(refreshTokenCookie)
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
    @DisplayName("Deve retornar 409 Conflict quando a conta do usuário estiver desativada")
    void refreshToken_shouldReturn409_whenUserAccountIsDisabled() {
      // Arrange
      mockPersistUser(defaultStatus);
      LoginRequestDto loginRequest = new LoginRequestDto(defaultUsername, defaultPassword);
      Cookie refreshTokenCookie =
          given()
              .spec(specification)
              .body(loginRequest)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .detailedCookie("refreshToken");

      alterAccountStatus(AccountStatus.DISABLED);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .cookie(refreshTokenCookie)
              .when()
              .post("/refresh-token")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Atenção: Sua conta está desativada.");
      assertThat(response.path()).isEqualTo("/api/auth/refresh-token");
      assertThat(response.fieldErrors()).isNull();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/signup")
  class SignupTests {

    @Test
    @DisplayName("Deve retornar 201 Created quando os dados do usuário forem válidos")
    void signup_shouldReturn201_whenUserDataIsValid() {
      // Arrange
      var request =
          new SignupRequestDto(
              defaultUsername,
              defaultFullName,
              defaultPhone,
              defaultPassword,
              defaultConfirmPassword);

      // Act
      SignupResponseDto response =
          given()
              .spec(specification)
              .body(request)
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
      assertThat(response.identifier()).isEqualTo(request.phone());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando as senhas não coincidirem")
    void signup_shouldReturn400_whenPasswordsNotCoincide() {
      // Arrange
      String differentPassword = "differentPassword";
      var request =
          new SignupRequestDto(
              defaultUsername, defaultFullName, defaultPhone, defaultPassword, differentPassword);

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
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.path()).isEqualTo("/api/auth/signup");
      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("confirmPassword");
      assertThat(response.fieldErrors().getFirst().message())
          .isEqualTo("A senha de confirmação não corresponde à senha.");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o telefone informado tem formato inválido")
    void signup_shouldReturn400_whenPhoneNumberIsInvalidFormat() {
      // Arrange
      String invalidPhone = "+999123456789";
      var request =
          new SignupRequestDto(
              defaultUsername,
              defaultFullName,
              invalidPhone,
              defaultPassword,
              defaultConfirmPassword);

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
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.message())
          .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      assertThat(response.path()).isEqualTo("/api/auth/signup");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando o username já estiver em uso")
    void signup_shouldReturn409_whenUsernameAlreadyExists() {
      // Arrange
      mockPersistUser(defaultStatus);
      String newPhone = "+5583998765432";
      var request =
          new SignupRequestDto(
              defaultUsername, defaultFullName, newPhone, defaultPassword, defaultConfirmPassword);

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
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Esse nome de usuário já está em uso.");
      assertThat(response.path()).isEqualTo("/api/auth/signup");
      assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando o telefone já estiver em uso")
    void signup_shouldReturn409_whenPhoneAlreadyExists() {
      // Arrange
      mockPersistUser(defaultStatus);
      String newUsername = "new_user";
      var request =
          new SignupRequestDto(
              newUsername, defaultFullName, defaultPhone, defaultPassword, defaultConfirmPassword);

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
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.message()).isEqualTo("Esse número de telefone já está em uso.");
      assertThat(response.path()).isEqualTo("/api/auth/signup");
      assertThat(response.fieldErrors()).isNull();
    }
  }
}
