package com.projetoExtensao.arenaMafia.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.service.LoginServiceImp;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para LoginService")
public class LoginServiceImpTest {

  @Mock private AuthPort authPort;

  @InjectMocks private LoginServiceImp loginServiceImp;

  @Test
  @DisplayName("Deve chamar o AuthPort para autenticar o usuário e retornar o DTO com os tokens")
  void login_shouldCallAuthPortAndReturnTokenDtoWhenSuccessful() {
    // Arrange
    String username = "username";
    String password = "password";
    LoginRequestDto loginRequestDto = new LoginRequestDto(username, password);
    User user = User.create(username, "Username Test", "559123456789", "password_hash");
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(
            "username",
            LocalDateTime.now(), // cratedAt
            LocalDateTime.now().plusHours(2), // expiresAt
            "access_token",
            "refresh_token");

    when(authPort.authenticate(username, password)).thenReturn(user);
    when(authPort.getTokens(user.getUsername(), user.getRole()))
        .thenReturn(tokenResponseDto);

    // Act
    TokenResponseDto tokenResponse = loginServiceImp.login(loginRequestDto);

    // Assert
    assertThat(tokenResponse).isNotNull();
    assertThat(tokenResponse).isEqualTo(tokenResponseDto);

    verify(authPort, times(1)).authenticate(username, password);
    verify(authPort, times(1)).getTokens(user.getUsername(), user.getRole());
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
    assertThatThrownBy(() -> loginServiceImp.login(loginRequestDto))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Credenciais inválidas. Por favor, verifique seu usuário e senha.");

    verify(authPort, never()).getTokens(anyString(), any(RoleEnum.class));
  }
}
