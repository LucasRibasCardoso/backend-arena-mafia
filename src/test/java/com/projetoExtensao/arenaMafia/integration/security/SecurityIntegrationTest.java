package com.projetoExtensao.arenaMafia.integration.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para a Camada de Segurança")
public class SecurityIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private PendingPhoneChangePort pendingPhoneChangePort;

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

    ErrorCode errorCode = ErrorCode.SESSION_EXPIRED;

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.path()).isEqualTo("/api/users/me/profile");
    assertThat(response.errorCode()).isEqualTo(errorCode.name());
    assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
  }

  @Test
  @DisplayName("Deve retornar 401 ao usar token de usuário deletado")
  void protectedEndpoint_ShouldReturn401_whenUserIsDeletedAfterLogin() {
    // Arrange
    User persistedUser = mockPersistUser();
    AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
    deleteMockUser(persistedUser.getId());

    UpdateProfileRequestDto request = new UpdateProfileRequestDto("Novo Nome Completo");

    // Act & Assert
    ErrorResponseDto response =
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .body(request)
            .when()
            .patch("/profile")
            .then()
            .log()
            .all()
            .statusCode(401)
            .extract()
            .as(ErrorResponseDto.class);

    ErrorCode errorCode = ErrorCode.SESSION_EXPIRED;

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.path()).isEqualTo("/api/users/me/profile");
    assertThat(response.errorCode()).isEqualTo(errorCode.name());
    assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
  }

  @Nested
  @DisplayName("Deve retornar 401 Unauthorized quando o status da conta for inválido")
  class AccountStatusTests {

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"DISABLED", "LOCKED", "PENDING_VERIFICATION"})
    @DisplayName(
        "Deve retornar 401 Unauthorized para os status: DISABLED, LOCKED, PENDING_VERIFICATION")
    void protectedEndpoint_shouldReturn401_whenAccountStatusIsInvalid(AccountStatus invalidStatus) {
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

      String newPhone = "+5547992044567";
      pendingPhoneChangePort.save(mockUser.getId(), newPhone);

      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .when()
              .post("/phone/verification/resend")
              .then()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      assertThat(response.status()).isEqualTo(401);
      assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
      assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
    }
  }
}
