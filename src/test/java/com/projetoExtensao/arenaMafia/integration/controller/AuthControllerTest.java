package com.projetoExtensao.arenaMafia.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.TestIntegrationBaseConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Testes de Integração para a Autenticação")
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

    // Cria um usuário de teste no banco de dados
    String passwordEncoded = passwordEncoder.encode("123456");
    User user = User.create("testUser", "Test User", "5547912345678", passwordEncoded);
    user.activateAccount();
    userRepository.save(user);
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
      LoginRequestDto request = new LoginRequestDto("testUser", "123456");

      // Act
      TokenResponseDto response =
          given()
              .spec(specification)
              .body(request)
              .when()
              .post("/login")
              .then()
              .statusCode(200)
              .extract()
              .as(TokenResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.username()).isEqualTo("testUser");
      assertThat(response.accessToken()).isNotBlank();
      assertThat(response.refreshToken()).isNotBlank();
      assertThat(response.createdAt()).isNotNull();
      assertThat(response.expiresAt()).isNotNull();
      assertThat(response.expiresAt()).isAfter(response.createdAt());
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
  }
}
