package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.LogoutUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.LogoutUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.RefreshTokenInvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
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
public class LogoutUseCaseImpTest {

  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

  @InjectMocks private LogoutUseCaseImp logoutUseCase;

  @Test
  @DisplayName("Deve deletar o token quando um refreshToken válido e existente for fornecido")
  void execute_shouldDeleteToken_whenValidTokenIsProvided() {
    // Arrange
    String tokenValue = RefreshTokenVO.generate().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(tokenValue);
    RefreshToken refreshTokenInDb = mock(RefreshToken.class);

    when(refreshTokenRepository.findByToken(refreshTokenVO))
        .thenReturn(Optional.of(refreshTokenInDb));

    // Act
    logoutUseCase.execute(tokenValue);

    verify(refreshTokenRepository, times(1)).findByToken(refreshTokenVO);
    verify(refreshTokenRepository, times(1)).delete(refreshTokenInDb);
  }

  @Test
  @DisplayName(
      "Não deve fazer nada se o refreshToken for válido mas não for encontrado no repositório")
  void execute_shouldDoNothing_whenTokenIsNotFound() {
    // Arrange
    String tokenValue = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(tokenValue);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act
    logoutUseCase.execute(tokenValue);

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
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenInvalidFormatException se o formato do token for inválido")
  void execute_shouldThrowException_whenTokenFormatIsInvalid() {
    // Arrange
    String malformedToken = "isto-nao-e-um-uuid";

    // Act & Assert
    assertThatThrownBy(() -> logoutUseCase.execute(malformedToken))
        .isInstanceOf(RefreshTokenInvalidFormatException.class)
        .hasMessage("Formato inválido para o refresh token.");

    // Verify
    verifyNoInteractions(refreshTokenRepository);
  }
}
