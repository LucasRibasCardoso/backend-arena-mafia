package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.LogoutUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para LogoutUseCase")
public class LogoutUseCaseTest {

  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @InjectMocks private LogoutUseCaseImp logoutUseCase;

  private final RefreshTokenVO refreshTokenVO = RefreshTokenVO.generate();

  @Test
  @DisplayName("Deve realizar o logout deletando o refreshToken válido")
  void execute_shouldDeleteRefreshToken() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    RefreshToken refreshToken = TestDataProvider.createRefreshToken(user);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));

    // Act
    logoutUseCase.execute(refreshTokenVO);

    // Assert
    verify(refreshTokenRepository, times(1)).delete(refreshToken);
  }

  @Test
  @DisplayName("Não deve fazer nada se o refreshToken for válido mas não for encontrado no repositório")
  void execute_shouldDoNothing_whenTokenIsNotFound() {
    // Arrange
    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act
    logoutUseCase.execute(refreshTokenVO);

    // Assert
    verify(refreshTokenRepository, times(1)).findByToken(refreshTokenVO);
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Não deve fazer nada se o refreshToken for nulo")
  void execute_shouldDoNothing_whenTokenIsNull() {
    // Act
    assertDoesNotThrow(() -> logoutUseCase.execute(null));

    // Assert
    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }
}
