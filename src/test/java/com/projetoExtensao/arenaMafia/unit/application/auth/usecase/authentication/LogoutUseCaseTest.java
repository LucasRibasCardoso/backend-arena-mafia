package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.LogoutUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.RefreshTokenInvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para LogoutUseCase")
public class LogoutUseCaseTest {

  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

  @InjectMocks private LogoutUseCaseImp logoutUseCase;

  private RefreshToken createRefreshToken() {
    User user = User.create("username", "Full Name", "+558320548181", "passwordHash");
    return RefreshToken.create(7L, user);
  }

  @Test
  @DisplayName("Deve realizar o logout deletando o refreshToken válido")
  void execute_shouldDeleteRefreshToken() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);
    RefreshToken refreshToken = createRefreshToken();

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));

    // Act
    logoutUseCase.execute(token);

    // Assert
    verify(refreshTokenRepository, times(1)).findByToken(refreshTokenVO);
    verify(refreshTokenRepository, times(1)).delete(refreshToken);
  }

  @Test
  @DisplayName(
      "Não deve fazer nada se o refreshToken for válido mas não for encontrado no repositório")
  void execute_shouldDoNothing_whenTokenIsNotFound() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act
    logoutUseCase.execute(token);

    // Assert
    verify(refreshTokenRepository, times(1)).findByToken(refreshTokenVO);
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t", "\n"})
  @DisplayName(
      "Não deve interagir com o repositório se o refreshToken for nulo, vazio ou em branco")
  void execute_shouldDoNothing_whenTokenIsNullOrEmptyOrBlank(String invalidToken) {
    // Act
    logoutUseCase.execute(invalidToken);

    // Assert / Verify
    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenInvalidFormatException se o formato do token for inválido")
  void execute_shouldThrowRefreshTokenInvalidFormatException_whenTokenFormatIsInvalid() {
    // Arrange
    String malformedToken = "isto-nao-e-um-uuid";

    // Act & Assert
    assertThatThrownBy(() -> logoutUseCase.execute(malformedToken))
        .isInstanceOf(RefreshTokenInvalidFormatException.class)
        .hasMessage("Formato inválido para o refresh token.");

    // Verify
    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }
}
