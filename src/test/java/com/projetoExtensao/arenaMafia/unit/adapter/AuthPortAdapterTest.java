package com.projetoExtensao.arenaMafia.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.AuthAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para AuthPortAdapter")
public class AuthPortAdapterTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private JwtTokenProvider tokenProvider;

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
  @DisplayName("Deve delegar a chamada para o JwtTokenProvider e retornar os tokens")
  void getTokens_shouldDelegateCallToJwtTokenProvider() {
    // Arrange
    String username = "testuser";
    RoleEnum role = RoleEnum.ROLE_USER;
    TokenResponseDto expectedTokens =
        new TokenResponseDto(
            username, LocalDateTime.now(), LocalDateTime.now().plusHours(2), "access", "refresh");

    when(tokenProvider.getTokens(username, role)).thenReturn(expectedTokens);

    // Act
    TokenResponseDto actualTokens = authAdapter.getTokens(username, role);

    // Assert
    assertThat(actualTokens).isEqualTo(expectedTokens);
    verify(tokenProvider, times(1)).getTokens(username, role);
  }

  @Test
  @DisplayName(
      "getRefreshToken: Deve delegar a chamada para o JwtTokenProvider e retornar os tokens atualizados")
  void getRefreshToken_shouldDelegateCallToJwtTokenProvider() {
    // Arrange
    String username = "testuser";
    RoleEnum role = RoleEnum.ROLE_USER;
    TokenResponseDto expectedTokens =
        new TokenResponseDto(
            username, LocalDateTime.now(), LocalDateTime.now().plusHours(2), "access", "refresh");

    when(tokenProvider.getTokens(username, role)).thenReturn(expectedTokens);

    // Act
    TokenResponseDto actualTokens = authAdapter.getRefreshToken(username, role);

    // Assert
    assertThat(actualTokens).isEqualTo(expectedTokens);
    verify(tokenProvider, times(1)).getTokens(username, role);
  }
}
