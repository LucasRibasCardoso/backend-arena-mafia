package com.projetoExtensao.arenaMafia.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para LoginService")
public class LoginUseCaseImpTest {

  @Mock private AuthPort authPort;

  @InjectMocks private LoginUseCaseImp loginUseCaseImp;

  @Test
  @DisplayName("Deve chamar o AuthPort para autenticar o usuário e retornar o DTO com os tokens")
  void login_shouldCallAuthPortAndReturnTokenDtoWhenSuccessful() {
    // Arrange
    String username = "username";
    String password = "password";
    LoginRequestDto loginRequestDto = new LoginRequestDto(username, password);
    User user = User.create(username, "Username Test", "559123456789", "password_hash");
    AuthResult tokenResponseDto = new AuthResult("username", "access_token", "refresh_token");

    when(authPort.authenticate(username, password)).thenReturn(user);
    when(authPort.generateTokens(user)).thenReturn(tokenResponseDto);

    // Act
    AuthResult tokenResponse = loginUseCaseImp.login(loginRequestDto);

    // Assert
    assertThat(tokenResponse).isNotNull();
    assertThat(tokenResponse).isEqualTo(tokenResponseDto);

    verify(authPort, times(1)).authenticate(username, password);
    verify(authPort, times(1)).generateTokens(user);
  }

  @Test
  @DisplayName("Deve propagar uma BadCredentialsException quando a autenticação falhar")
  void login_ShouldPropagateBadCredentialsExceptionWhenAuthenticationFails() {
    // Arrange
    LoginRequestDto loginRequestDto = new LoginRequestDto("username", "passwordWrong");

    when(authPort.authenticate(loginRequestDto.username(), loginRequestDto.password()))
        .thenThrow(
            new BadCredentialsException(
                "Credenciais inválidas. Por favor, verifique seu usuário e senha."));

    // Act & Assert
    assertThatThrownBy(() -> loginUseCaseImp.login(loginRequestDto))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Credenciais inválidas. Por favor, verifique seu usuário e senha.");

    verify(authPort, never()).generateTokens(any(User.class));
  }
}
