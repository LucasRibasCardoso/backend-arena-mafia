package com.projetoExtensao.arenaMafia.integration.controller.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.response.UserProfileResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para UserController")
public class UserControllerTest extends WebIntegrationTestConfig {

  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private PendingPhoneChangePort pendingPhoneChangePort;
  @Autowired private OtpPort otpPort;

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
  @DisplayName("Teste para o endpoint /api/users/me")
  class GetMyProfileTest {

    @Test
    @DisplayName("Deve retornar 200 OK com os detalhes do perfil do usuário")
    void getMyProfile_shouldReturn200_withUserProfileDetails() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(UserProfileResponseDto.class);

      assertThat(response.username()).isEqualTo(mockUser.getUsername());
      assertThat(response.fullName()).isEqualTo(mockUser.getFullName());
      assertThat(response.phone()).isEqualTo(mockUser.getPhone());
      assertThat(response.role()).isEqualTo(mockUser.getRole().name());
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /api/users/me/profile")
  class UpdateProfileTest {

    @Test
    @DisplayName("Deve retornar 200 OK quando a atualização for bem-sucedida")
    void updateProfile_shouldReturn200_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new UpdateProfileRequestDto("Novo Nome Completo");

      // Act & Assert
      given()
          .spec(specification)
          .header("Authorization", "Bearer " + tokens.accessToken())
          .body(request)
          .when()
          .patch("/profile")
          .then()
          .statusCode(200);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(updatedUser.getFullName()).isEqualTo(request.fullName());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o nome completo for inválido no DTO")
    void updateProfile_shouldReturn400_whenFullNameIsEmptyOrNull() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new UpdateProfileRequestDto("  ");

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .patch("/profile")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("fullName");

      User unchangedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(unchangedUser.getFullName()).isEqualTo(defaultFullName);
    }
  }

  @Nested
  @DisplayName("Teste para o endpoint /api/users/me/username")
  class ChangeUsernameTest {

    @Test
    @DisplayName("Deve retornar 200 OK quando o nome de usuário for alterado com sucesso")
    void changeUsername_shouldReturn200_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangeUsernameRequestDto("new_username");

      // Act & Assert
      given()
          .spec(specification)
          .header("Authorization", "Bearer " + tokens.accessToken())
          .body(request)
          .when()
          .patch("/username")
          .then()
          .statusCode(200);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(updatedUser.getUsername()).isEqualTo(request.username());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Deve retornar 400 Bad Request quando o nome de usuário for inválido no DTO")
    void changeUsername_shouldReturn400_whenUsernameIsEmptyOrNull(String invalidUsername) {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangeUsernameRequestDto(invalidUsername);

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .patch("/username")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("username");

      User unchangedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(unchangedUser.getUsername()).isEqualTo(defaultUsername);
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando o nome de usuário já estiver em uso")
    void changeUsername_shouldReturn409_whenUsernameAlreadyExists() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      User mockExistingUser =
          mockPersistUser("existing_user", "Existing User", "+5521921340987", "123456");

      var request = new ChangeUsernameRequestDto(mockExistingUser.getUsername());

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .patch("/username")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      User unchangedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(unchangedUser.getUsername()).isEqualTo(defaultUsername);
      assertThat(response.message()).isEqualTo("Esse nome de usuário já está em uso.");
    }
  }

  @Nested
  @DisplayName("Teste para o endpoint /api/users/me/password")
  class ChangePasswordTest {
    @Test
    @DisplayName("Deve retornar 204 No Content quando a alteração de senha for bem-sucedida")
    void changePassword_shouldReturn204_whenSuccessful() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangePasswordRequestDto(defaultPassword, "new_password", "new_password");

      // Act & Assert
      given()
          .spec(specification)
          .header("Authorization", "Bearer " + tokens.accessToken())
          .body(request)
          .when()
          .post("/password")
          .then()
          .statusCode(204);

      User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(passwordEncoder.matches("new_password", updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName(
        "Deve retornar 400 Bad Request quando a nova senha e a confirmação não corresponderem")
    void changePassword_shouldReturn400_whenNewPasswordAndConfirmationDoNotMatch() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request =
          new ChangePasswordRequestDto(defaultPassword, "new_password", "different_password");

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .post("/password")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      assertThat(response.message())
          .isEqualTo("Erro de validação. Verifique os campos informados.");
      assertThat(response.fieldErrors()).hasSize(1);
      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("confirmPassword");

      User unchangedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(passwordEncoder.matches(defaultPassword, unchangedUser.getPasswordHash()))
          .isTrue();
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a senha atual estiver incorreta")
    void changePassword_shouldReturn400_whenCurrentPasswordIsIncorrect() {
      // Arrange
      User mockUser = mockPersistUser();
      AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
      var request = new ChangePasswordRequestDto("wrong_password", "new_password", "new_password");

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + tokens.accessToken())
              .body(request)
              .when()
              .post("/password")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      assertThat(response.message()).isEqualTo("A Senha atual está incorreta");

      User unchangedUser = userRepository.findById(mockUser.getId()).orElseThrow();
      assertThat(passwordEncoder.matches(defaultPassword, unchangedUser.getPasswordHash()))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /api/users/me/phone/verification")
  class ChangePhoneTest {

    @Nested
    @DisplayName("Etapa 1: Iniciar alteração de telefone")
    class InitiateChangePhoneTest {
      @Test
      @DisplayName("Deve retornar 202 Accepted quando a alteração de telefone iniciar com sucesso")
      void initiateChangePhone_shouldReturn202_whenSuccessful() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new InitiateChangePhoneRequestDto("+5547992044567");

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .body(request)
            .when()
            .post("/phone/verification")
            .then()
            .statusCode(202);
      }

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("Deve retornar 400 Bad Request quando o número de telefone for inválido no DTO")
      void initiateChangePhone_shouldReturn400_whenPhoneIsEmptyOrNull(String invalidPhone) {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new InitiateChangePhoneRequestDto(invalidPhone);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/phone/verification")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        assertThat(response.message())
            .isEqualTo("Erro de validação. Verifique os campos informados.");
        assertThat(response.fieldErrors()).hasSize(1);
        assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("newPhone");
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando o número de telefone for inválido")
      void initiateChangePhone_shouldReturn400_whenPhoneIsInvalid() {
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        var request = new InitiateChangePhoneRequestDto("+999999999999");

        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/phone/verification")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        assertThat(response.message())
            .isEqualTo("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando o número de telefone já está em uso")
      void initiateChangePhone_shouldReturn409_whenPhoneAlreadyExists() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        User mockExistingUser =
            mockPersistUser("existing_user", "Existing User", "+5521921340987", "123456");
        var request = new InitiateChangePhoneRequestDto(mockExistingUser.getPhone());

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .post("/phone/verification")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        assertThat(response.message()).isEqualTo("Esse número de telefone já está em uso.");
      }
    }

    @Nested
    @DisplayName("Etapa 2: Completar alteração de telefone")
    class CompleteChangePhoneTest {

      @Test
      @DisplayName("Deve retornar 200 OK quando a verificação de telefone for um sucesso")
      void completeChangePhone_shouldReturn200_whenSuccessful() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        String newPhone = "+5547992044567";
        pendingPhoneChangePort.save(mockUser.getId(), newPhone);
        OtpCode verificationCode = otpPort.generateOtpCode(mockUser.getId());
        var request = new CompletePhoneChangeRequestDto(verificationCode);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .body(request)
            .when()
            .patch("/phone/verification/confirm")
            .then()
            .statusCode(200);

        User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
        assertThat(updatedUser.getPhone()).isEqualTo(newPhone);
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando o código de verificação for inválido")
      void completeChangePhone_shouldReturn400_whenCodeIsIncorrect() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        String newPhone = "+5547992044567";
        pendingPhoneChangePort.save(mockUser.getId(), newPhone);

        OtpCode otpCode = OtpCode.generate();
        var request = new CompletePhoneChangeRequestDto(otpCode);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .patch("/phone/verification/confirm")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        assertThat(response.message()).isEqualTo("Código de verificação inválido ou expirado.");

        User unchangedUser = userRepository.findById(mockUser.getId()).orElseThrow();
        assertThat(unchangedUser.getPhone()).isEqualTo(defaultPhone);
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando a solicitação de alteração expirar")
      void completeChangePhone_shouldReturn400_whenRequestHasExpired() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        OtpCode otpCode = OtpCode.generate();
        var request = new CompletePhoneChangeRequestDto(otpCode);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(request)
                .when()
                .patch("/phone/verification/confirm")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        assertThat(response.message())
            .isEqualTo("Sua solicitação de alteração de telefone já expirou. Tente novamente.");
      }
    }

    @Nested
    @DisplayName("Reenviar código OTP caso não recebido")
    class ResendChangePhoneOtpTest {

      @Test
      @DisplayName("Deve retornar 204 No Content quando o código OTP for reenviado com sucesso")
      void resendChangePhoneOtp_shouldReturn204_whenSuccessful() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        String newPhone = "+5547992044567";
        pendingPhoneChangePort.save(mockUser.getId(), newPhone);

        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .when()
            .post("/phone/verification/resend")
            .then()
            .statusCode(204);
      }

      @Test
      @DisplayName("Deve retornar 404 Not Found quando não houver alteração de telefone pendente")
      void resendChangePhoneOtp_shouldReturn404_whenNoPendingPhoneChange() {
        // Arrange
        mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .when()
                .post("/phone/verification/resend")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.message())
            .isEqualTo("Nenhuma alteração de telefone pendente encontrada para o usuário.");
      }

      @Nested
      @DisplayName("Deve lançar exceção quando a conta do usuário não está ativada")
      class AccountStatusTests {
        @Test
        @DisplayName("Deve retornar 409 Conflict quando a conta está desativada")
        void resendChangePhoneOtp_shouldReturn409_whenUserAccountIsDisabled() {
          // Arrange
          User mockUser = mockPersistUser();
          AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

          mockUser.disableAccount();
          userRepository.save(mockUser);

          String newPhone = "+5547992044567";
          pendingPhoneChangePort.save(mockUser.getId(), newPhone);

          var response =
              given()
                  .spec(specification)
                  .header("Authorization", "Bearer " + tokens.accessToken())
                  .when()
                  .post("/phone/verification/resend")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertThat(response.message())
              .isEqualTo(
                  "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando a conta está bloqueada")
        void resendChangePhoneOtp_shouldReturn409_whenUserAccountIsLocked() {
          // Arrange
          User mockUser = mockPersistUser();
          AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

          mockUser.lockAccount();
          userRepository.save(mockUser);

          String newPhone = "+5547992044567";
          pendingPhoneChangePort.save(mockUser.getId(), newPhone);

          var response =
              given()
                  .spec(specification)
                  .header("Authorization", "Bearer " + tokens.accessToken())
                  .when()
                  .post("/phone/verification/resend")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertThat(response.message())
              .isEqualTo("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando a conta está pendente de verificação")
        void resendChangePhoneOtp_shouldReturn409_whenUserAccountIsPendingVerification() {
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
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertThat(response.message())
              .isEqualTo(
                  "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
        }
      }
    }

    @Nested
    @DisplayName("Teste para o endpoint /api/users/me/disable")
    class DisableMyAccountTest {

      @Test
      @DisplayName("Deve retornar 204 No Content quando a conta for desativada com sucesso")
      void disableMyAccount_shouldReturn204_whenSuccessful() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", "Bearer " + tokens.accessToken())
            .when()
            .post("/disable")
            .then()
            .statusCode(204);

        User updatedUser = userRepository.findById(mockUser.getId()).orElseThrow();
        assertThat(updatedUser.isEnabled()).isFalse();
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando a conta já está desativada")
      void disableMyAccount_shouldReturn409_whenAccountAlreadyDisabled() {
        // Arrange
        User mockUser = mockPersistUser();
        AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
        mockUser.disableAccount();
        userRepository.save(mockUser);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", "Bearer " + tokens.accessToken())
                .when()
                .post("/disable")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        assertThat(response.message())
            .isEqualTo("Sua conta precisa estar ativa para ser desativada.");
      }
    }
  }
}
