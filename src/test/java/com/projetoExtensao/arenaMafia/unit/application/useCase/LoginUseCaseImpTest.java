package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountLockedException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountNotVerifiedException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.DisabledAccountException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
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
public class LoginUseCaseImpTest {

  @Mock private AuthPort authPort;

  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private LoginUseCaseImp loginUseCaseImp;

  @Test
  @DisplayName("Deve autenticar o usuário e retornar o DTO com os tokens")
  void execute_shouldCallAuthPortAndReturnTokenDtoWhenSuccessful() {
    // Arrange
    String username = "username";
    String password = "password";
    LoginRequestDto loginRequestDto = new LoginRequestDto(username, password);
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            "username",
            "Username Test",
            "559123456789",
            "password_hash",
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            Instant.now());
    AuthResult tokenResponseDto = new AuthResult("username", "access_token", "refresh_token");

    when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));
    when(authPort.authenticate(username, password)).thenReturn(user);
    when(authPort.generateTokens(user)).thenReturn(tokenResponseDto);

    // Act
    AuthResult tokenResponse = loginUseCaseImp.execute(loginRequestDto);

    // Assert
    assertThat(tokenResponse).isNotNull();
    assertThat(tokenResponse).isEqualTo(tokenResponseDto);

    verify(authPort, times(1)).authenticate(username, password);
    verify(authPort, times(1)).generateTokens(user);
  }

  @Test
  @DisplayName("Deve lançar BadCredentialsException quando a autenticação falhar")
  void execute_ShouldPropagateBadCredentialsExceptionWhenAuthenticationFails() {
    // Arrange
    LoginRequestDto loginRequestDto = new LoginRequestDto("username", "passwordWrong");
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            "username",
            "Username Test",
            "559123456789",
            "password_hash",
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            Instant.now());

    when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));
    when(authPort.authenticate(loginRequestDto.username(), loginRequestDto.password()))
        .thenThrow(
            new BadCredentialsException(
                "Credenciais inválidas. Por favor, verifique seu usuário e senha."));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCaseImp.execute(loginRequestDto))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Credenciais inválidas. Por favor, verifique seu usuário e senha.");

    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar BadCredentialsException quando não encontrar o usuário para autenticar")
  void execute_shouldThrowBadCredentialsExceptionWhenUserNotFound() {
    // Arrange
    LoginRequestDto loginRequestDto = new LoginRequestDto("nonexistentUser", "password");
    when(userRepository.findByUsername("nonexistentUser")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> loginUseCaseImp.execute(loginRequestDto))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Credenciais inválidas. Por favor, verifique seu usuário e senha.");

    // Verify
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountNotVerifiedException quando o status do usuário for PENDING_VERIFICATION")
  void execute_shouldThrowAccountNotVerifiedExceptionForPendingVerificationStatus() {
    // Arrange
    LoginRequestDto loginRequestDto = new LoginRequestDto("testUser", "123456");
    User pendingUser = createUserWithStatus(AccountStatus.PENDING_VERIFICATION);
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(pendingUser));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCaseImp.execute(loginRequestDto))
        .isInstanceOf(AccountNotVerifiedException.class)
        .hasMessage("Sua conta ainda não foi verificada.");

    // Verify
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar AccountLockedException quando o status do usuário for LOCKED")
  void execute_shouldThrowAccountLockedExceptionForLockedStatus() {
    // Arrange
    LoginRequestDto loginRequestDto = new LoginRequestDto("testUser", "123456");
    User lockedUser = createUserWithStatus(AccountStatus.LOCKED);
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(lockedUser));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCaseImp.execute(loginRequestDto))
        .isInstanceOf(AccountLockedException.class)
        .hasMessage("Sua conta está bloqueada. Por favor, contate o suporte.");

    // Verify
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar DisabledAccountException quando o status do usuário for diferente de ACTIVE, PENDING_VERIFICATION ou LOCKED")
  void execute_shouldThrowDisabledAccountExceptionForOtherStatuses() {
    // Arrange
    LoginRequestDto loginRequestDto = new LoginRequestDto("testUser", "123456");
    User disabledUser = createUserWithStatus(AccountStatus.DISABLED);
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(disabledUser));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCaseImp.execute(loginRequestDto))
        .isInstanceOf(DisabledAccountException.class)
        .hasMessage("Está conta não está ativa.");

    // Verify
    verify(authPort, never()).authenticate(anyString(), anyString());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  private User createUserWithStatus(AccountStatus status) {
    return User.reconstitute(
        java.util.UUID.randomUUID(),
        "testUser",
        "Test User",
        "5547912345678",
        "password_hash",
        status,
        RoleEnum.ROLE_USER,
        Instant.now());
  }
}
