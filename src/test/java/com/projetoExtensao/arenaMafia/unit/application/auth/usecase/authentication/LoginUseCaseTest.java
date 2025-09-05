package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para LoginUseCase")
public class LoginUseCaseTest {

  @Mock private AuthPort authPort;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private LoginUseCaseImp loginUseCase;

  private final String defaultUsername = "testuser";
  private final String defaultPassword = "password123";

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "hashedPassword",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve autenticar o usuário com sucesso e retornar os tokens de acesso e refresh")
  void execute_shouldAuthenticateUserAndReturnTokens() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);
    AuthResult expectedResponse = new AuthResult(user, "access_token", "refresh_token");

    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(user));
    when(authPort.authenticate(defaultUsername, defaultPassword)).thenReturn(user);
    when(authPort.generateTokens(user)).thenReturn(expectedResponse);

    // Act
    AuthResult response = loginUseCase.execute(request);

    // Assert
    assertThat(response.user()).isEqualTo(expectedResponse.user());
    assertThat(response.accessToken()).isEqualTo(expectedResponse.accessToken());
    assertThat(response.refreshToken()).isEqualTo(expectedResponse.refreshToken());

    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(authPort, times(1)).authenticate(defaultUsername, defaultPassword);
    verify(authPort, times(1)).generateTokens(user);
  }

  @Test
  @DisplayName("Deve lançar BadCredentialsException quando não encontrar o usuário para autenticar")
  void execute_shouldThrowBadCredentialsException_whenUserNotFound() {
    // Arrange
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);
    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Credenciais inválidas. Por favor, verifique seu usuário e senha.");

    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar BadCredentialsException quando a autenticação falhar")
  void execute_ShouldThrowBadCredentialsException_whenAuthenticationFails() {
    // Arrange
    String errorMessage = "Credenciais inválidas. Por favor, verifique seu usuário e senha.";
    User user = createUser(AccountStatus.ACTIVE);
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(user));
    doThrow(new BadCredentialsException(errorMessage))
        .when(authPort)
        .authenticate(defaultUsername, defaultPassword);

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage(errorMessage);

    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(authPort, times(1)).authenticate(defaultUsername, defaultPassword);
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver pendente de verificação")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsPending() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);

    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver bloqueada")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsLocked() {
    // Arrange
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);
    User user = createUser(AccountStatus.LOCKED);

    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver desativada")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsDisabled() {
    // Arrange
    LoginRequestDto request = new LoginRequestDto(defaultUsername, defaultPassword);
    User user = createUser(AccountStatus.DISABLED);

    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está desativada.");

    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }
}
