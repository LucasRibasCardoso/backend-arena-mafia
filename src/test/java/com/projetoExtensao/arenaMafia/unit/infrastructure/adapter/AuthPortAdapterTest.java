package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultPassword;
import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultUsername;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.AccountStatusAuthenticationException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidCredentialsException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.AuthAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    User user = TestDataProvider.createActiveUser();
    UserDetailsAdapter userDetails = new UserDetailsAdapter(user);
    Authentication mockAuthentication = mock(Authentication.class);

    when(mockAuthentication.getPrincipal()).thenReturn(userDetails);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);

    // Act
    User actualUser = authAdapter.authenticate(defaultUsername, defaultPassword);

    // Assert
    assertThat(actualUser).isNotNull();
    assertThat(actualUser).isEqualTo(userDetails.getUser());

    verify(authenticationManager, times(1))
        .authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidCredentialsException ao falhar autenticação")
  void authenticate_shouldThrowInvalidCredentialsExceptionOnAuthFailure() {
    // Arrange
    doThrow(InvalidCredentialsException.class)
        .when(authenticationManager)
        .authenticate(any(UsernamePasswordAuthenticationToken.class));

    // Act & Assert
    assertThatThrownBy(() -> authAdapter.authenticate("wronguser", "wrongpass"))
        .isInstanceOf(InvalidCredentialsException.class)
        .satisfies(
            ex -> {
              InvalidCredentialsException exception = (InvalidCredentialsException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            });
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar AccountStatusAuthenticationException para contas não ativadas")
  void authenticate_shouldThrowUnauthorizedExceptionForNonActiveUsers(
      AccountStatus accountStatus, ErrorCode expectedErrorCode) {
    // Arrange
    var accountStatusException = new AccountStatusAuthenticationException(expectedErrorCode);
    var wrapperException = new BadCredentialsException("Usuário não ativo", accountStatusException);

    doThrow(wrapperException)
        .when(authenticationManager)
        .authenticate(any(UsernamePasswordAuthenticationToken.class));

    // Act & Assert
    assertThatThrownBy(() -> authAdapter.authenticate(defaultUsername, defaultPassword))
        .isInstanceOf(AccountStatusAuthenticationException.class)
        .satisfies(
            ex -> {
              AccountStatusAuthenticationException exception =
                  (AccountStatusAuthenticationException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
            });
  }

  @Test
  @DisplayName("Deve gerar tokens com sucesso, deletando o antigo e criando novos")
  void generateTokens_ShouldSucceed() {
    // Arrange
    String accessToken = "fake-access-token";
    User user = User.create("testuser", "Test User", "+5547912345678", "hash");
    ReflectionTestUtils.setField(authAdapter, "refreshTokenExpirationDays", 30L);

    RefreshToken refreshToken = RefreshToken.create(30L, user);
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
    when(tokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole()))
        .thenReturn(accessToken);

    // Act
    AuthResult result = authAdapter.generateTokens(user);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.user()).isNotNull();
    assertThat(result.user().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.user().getPhone()).isEqualTo(user.getPhone());
    assertThat(result.user().getFullName()).isEqualTo(user.getFullName());
    assertThat(result.user().getRole()).isEqualTo(user.getRole());
    assertThat(result.accessToken()).isEqualTo(accessToken);
    assertThat(result.refreshToken()).isEqualTo(refreshToken.getToken());

    verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    verify(refreshTokenRepository, times(1)).deleteByUser(user);
    verify(tokenProvider, times(1))
        .generateAccessToken(user.getId(), user.getUsername(), user.getRole());
  }
}
