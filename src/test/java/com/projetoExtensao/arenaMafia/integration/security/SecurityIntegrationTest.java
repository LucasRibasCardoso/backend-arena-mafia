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

  @Nested
  @DisplayName("Deve retornar 401 Unauthorized quando o status conta for inválido")
  class AccountStatusTests {
    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando a conta está desativada")
    void resendChangePhoneOtp_shouldReturn401_whenUserAccountIsDisabled() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      alterAccountStatus(mockUser.getId(), AccountStatus.DISABLED);

      String newPhone = "+5547992044567";
      pendingPhoneChangePort.save(mockUser.getId(), newPhone);

      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .when()
              .post("/phone/verification/resend")
              .then()
              .log()
              .all()
              .statusCode(401)
              .extract()
              .as(ErrorResponseDto.class);

      // Assert
      ErrorCode errorCode = ErrorCode.ACCOUNT_DISABLED;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/resend");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando a conta está bloqueada")
    void resendChangePhoneOtp_shouldReturn401_whenUserAccountIsLocked() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      alterAccountStatus(mockUser.getId(), AccountStatus.LOCKED);

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
      ErrorCode errorCode = ErrorCode.ACCOUNT_LOCKED;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/resend");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando a conta está pendente de verificação")
    void resendChangePhoneOtp_shouldReturn401_whenUserAccountIsPendingVerification() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      alterAccountStatus(mockUser.getId(), AccountStatus.PENDING_VERIFICATION);

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
      ErrorCode errorCode = ErrorCode.ACCOUNT_PENDING_VERIFICATION;

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.path()).isEqualTo("/api/users/me/phone/verification/resend");
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
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
}
