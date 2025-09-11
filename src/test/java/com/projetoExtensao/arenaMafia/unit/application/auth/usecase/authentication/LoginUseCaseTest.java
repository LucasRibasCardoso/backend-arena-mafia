package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidCredentialsException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.UnauthorizedException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para LoginUseCase")
public class LoginUseCaseTest {

  @Mock private AuthPort authPort;
  @InjectMocks private LoginUseCaseImp loginUseCase;

  private final String defaultUsername = TestDataProvider.defaultUsername;
  private final String defaultPassword = TestDataProvider.defaultPassword;

  @Test
  @DisplayName("Deve autenticar o usuário com sucesso e retornar os tokens de acesso e refresh")
  void execute_shouldAuthenticateUserAndReturnTokens() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.generate();
    AuthResult expectedResponse = new AuthResult(user, "access_token", refreshTokenVO);

    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

    when(authPort.authenticate(request.username(), request.password())).thenReturn(user);
    when(authPort.generateTokens(user)).thenReturn(expectedResponse);

    // Act
    AuthResult response = loginUseCase.execute(request);

    // Assert
    assertThat(response.user()).isEqualTo(expectedResponse.user());
    assertThat(response.accessToken()).isEqualTo(expectedResponse.accessToken());
    assertThat(response.refreshToken()).isEqualTo(expectedResponse.refreshToken());
  }

  @Test
  @DisplayName("Deve lançar InvalidCredentialsException quando as credenciais são inválidas")
  void execute_shouldThrowInvalidCredentialsException_whenCredentialsIsInvalid() {
    // Arrange
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

    doThrow(new InvalidCredentialsException())
        .when(authPort)
        .authenticate(request.username(), request.password());

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(InvalidCredentialsException.class)
        .satisfies(
            ex -> {
              InvalidCredentialsException exception = (InvalidCredentialsException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            });

    verify(authPort, never()).generateTokens(any(User.class));
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar UnauthorizedException quando a conta não está ativa")
  void execute_shouldThrowInvalidCredentialsException_whenAccountIsNotActive(
      AccountStatus invalidStatus, ErrorCode expectedErrorCode) {
    // Arrange
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

    doThrow(new UnauthorizedException(expectedErrorCode))
        .when(authPort)
        .authenticate(request.username(), request.password());

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(UnauthorizedException.class)
        .satisfies(
            ex -> {
              UnauthorizedException exception = (UnauthorizedException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
            });

    verify(authPort, never()).generateTokens(any(User.class));
  }
}
