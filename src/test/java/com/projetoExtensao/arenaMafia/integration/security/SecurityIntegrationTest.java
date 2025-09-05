package com.projetoExtensao.arenaMafia.integration.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDTO;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para a Camada de Segurança")
public class SecurityIntegrationTest extends WebIntegrationTestConfig {

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

  @Test
  @DisplayName("Deve retornar 401 ao tentar acessar endpoint protegido sem token")
  void protectedEndpoint_ShouldReturn401_whenNoToken() {
    // Arrange & Act & Assert
    ErrorResponseDto response =
        given()
            .spec(specification)
            .when()
            .patch("/profile")
            .then()
            .statusCode(401)
            .extract()
            .as(ErrorResponseDto.class);

    assertThat(response.message())
        .isEqualTo("Sua sessão é inválida ou expirou. Por favor, faça login novamente.");
  }

  @Test
  @DisplayName("Deve retornar 401 ao usar token de usuário deletado")
  void protectedEndpoint_ShouldReturn401_whenUserIsDeletedAfterLogin() {
    // Arrange
    User persistedUser = mockPersistUser();
    AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
    deleteMockUser(persistedUser.getId());

    UpdateProfileRequestDTO request = new UpdateProfileRequestDTO("Novo Nome Completo");

    // Act & Assert
    ErrorResponseDto response =
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .body(request)
            .when()
            .patch("/profile")
            .then()
            .statusCode(401)
            .extract()
            .as(ErrorResponseDto.class);

    assertThat(response.message())
        .isEqualTo("Sua sessão é inválida ou expirou. Por favor, faça login novamente.");
  }
}
