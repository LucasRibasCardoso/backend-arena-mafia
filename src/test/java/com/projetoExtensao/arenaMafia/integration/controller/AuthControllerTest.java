package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
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
    @DisplayName("Deve retornar 401 Unauthorized para login com credenciais inválidas")
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
    @DisplayName("Deve retornar 403 Forbidden para usuário com status PENDING_VERIFICATION")
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
    @DisplayName("Deve retornar 403 Forbidden para usuário com status LOCKED")
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
  }

  @Nested
  @DisplayName("Testes para o endpoint /auth/refresh-token")
  class RefreshTokenTests {

    @Test
    @DisplayName("Deve retornar 200 OK com novos tokens ao usar um refreshToken válido")
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
    @DisplayName("Deve retornar 401 Unauthorized para um refreshToken inválido")
    void refreshToken_shouldReturn401ForInvalidToken() {
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
              .statusCode(401)
              .extract()
              .response();

      // Assert
      ErrorResponseDto errorResponse = response.as(ErrorResponseDto.class);
      assertThat(errorResponse).isNotNull();
      assertThat(errorResponse.status()).isEqualTo(401);
      assertThat(errorResponse.message())
          .isEqualTo("Token de atualização inválido ou expirado. Por favor, faça login novamente.");
      assertThat(errorResponse.timestamp()).isNotNull();
      assertThat(errorResponse.path()).isEqualTo("/api/auth/refresh-token");
    }
  }

  private void createAndPersistUser(AccountStatus status) {
    String passwordEncoded = passwordEncoder.encode("123456");
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            "testUser",
            "Test User",
            "5547912345678",
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            Instant.now());

    userRepository.save(user);
  }
}
