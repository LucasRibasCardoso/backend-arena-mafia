package com.projetoExtensao.arenaMafia.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthResult;
import com.projetoExtensao.arenaMafia.application.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.AuthAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para AuthPortAdapter")
public class AuthPortAdapterTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private JwtTokenProvider tokenProvider;

  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

  @InjectMocks private AuthAdapter authAdapter;

  @Test
  @DisplayName("Deve chamar o AuthenticationManager e retornar o User logado")
  void authenticate_shouldCallAuthenticationManagerAndReturnUser() {
    // Arrange
    String username = "testuser";
    String password = "password123";

    User expectedUser = User.create(username, "Test User", "5547912345678", "hash");
    UserDetailsAdapter userDetails = new UserDetailsAdapter(expectedUser);
    Authentication mockAuthentication = mock(Authentication.class);

    when(mockAuthentication.getPrincipal()).thenReturn(userDetails);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);

    // Act
    User actualUser = authAdapter.authenticate(username, password);

    // Assert
    assertThat(actualUser).isNotNull();
    assertThat(actualUser).isEqualTo(expectedUser);

    verify(authenticationManager, times(1))
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  @DisplayName("Deve gerar tokens com sucesso, deletando o antigo e criando novos")
  void generateTokens_ShouldSucceed() {
    // Arrange
    String expectedAccessToken = "fake-access-token";
    User user = User.create("testuser", "Test User", "5547912345678", "hash");
    ReflectionTestUtils.setField(authAdapter, "refreshTokenExpirationDays", 30L);

    RefreshToken refreshToken = RefreshToken.create(30L, user);
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
    when(tokenProvider.generateAccessToken(user.getUsername(), user.getRole()))
        .thenReturn(expectedAccessToken);

    // Act
    AuthResult result = authAdapter.generateTokens(user);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.username()).isEqualTo(user.getUsername());
    assertThat(result.accessToken()).isEqualTo(expectedAccessToken);
    assertThat(result.refreshToken()).isNotNull();

    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    verify(refreshTokenRepository, times(1)).deleteByUser(user);
    verify(tokenProvider, times(1)).generateAccessToken(user.getUsername(), user.getRole());
  }
}
