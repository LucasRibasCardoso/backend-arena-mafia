package com.projetoExtensao.arenaMafia.integration.controller.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDTO;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.response.UserProfileResponseDTO;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para UserController")
public class UserControllerTest extends WebIntegrationTestConfig {

  @Autowired private UserRepositoryPort userRepository;

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
  @DisplayName("Testes para o endpoint /api/users/me/profile")
  class UpdateProfileTest {

    @Test
    @DisplayName("Deve retornar 200 OK quando a atualização for bem-sucedida")
    void updateProfile_ShouldReturn200_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      UpdateProfileRequestDTO request = new UpdateProfileRequestDTO("Novo Nome Completo");

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .patch("/profile")
              .then()
              .statusCode(200)
              .extract()
              .as(UserProfileResponseDTO.class);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();

      assertThat(updatedUser.getFullName()).isEqualTo(request.fullName());
      assertThat(response.fullName()).isEqualTo(request.fullName());
    }
  }
}
