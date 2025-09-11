package com.projetoExtensao.arenaMafia.unit.infrastructure.security;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultUsername;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.AccountStatusAuthenticationException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsServiceImpl;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para UserDetailsService")
public class UserDetailsServiceImpTest {

  @Mock private UserRepositoryPort userRepositoryPort;
  @InjectMocks private UserDetailsServiceImpl userDetailsService;

  @Nested
  @DisplayName("Testes para o método loadUserByUsername")
  class LoadUserByUsernameTests {
    @Test
    @DisplayName("Deve retornar UserDetails quando o usuário for encontrado pelo username")
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
      // Arrange
      User user = TestDataProvider.createActiveUser();

      when(userRepositoryPort.findByUsername(defaultUsername)).thenReturn(Optional.of(user));

      // Act
      UserDetails userDetails = userDetailsService.loadUserByUsername(defaultUsername);

      // Assert
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(defaultUsername);
      assertThat(userDetails).isInstanceOf(UserDetailsAdapter.class);
      verify(userRepositoryPort, times(1)).findByUsername(defaultUsername);
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o usuário não for encontrado")
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
      // Arrange
      when(userRepositoryPort.findByUsername(defaultUsername)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userDetailsService.loadUserByUsername(defaultUsername))
          .isInstanceOf(UsernameNotFoundException.class)
          .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
    @DisplayName(
        "Deve lançar AccountStatusAuthenticationException quando o status da conta não for ACTIVE")
    void loadUserByUsername_shouldThrowAccountStatusException_whenAccountNotActive(
        AccountStatus status, ErrorCode expectedError) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(status).build();

      when(userRepositoryPort.findByUsername(defaultUsername)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> userDetailsService.loadUserByUsername(defaultUsername))
          .isInstanceOf(AccountStatusAuthenticationException.class)
          .satisfies(
              ex -> {
                AccountStatusAuthenticationException exception =
                    (AccountStatusAuthenticationException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(expectedError);
              });
    }
  }

  @Nested
  @DisplayName("Testes para o método loadUserById")
  class LoadUserByIdTests {

    @Test
    @DisplayName("Deve retornar UserDetails quando o usuário for encontrado pelo ID")
    void loadUserById_shouldReturnUserDetails_whenUserExists() {
      // Arrange
      User user = TestDataProvider.createActiveUser();

      when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));

      // Act
      UserDetails userDetails = userDetailsService.loadUserById(user.getId());

      // Assert
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(defaultUsername);
      assertThat(userDetails).isInstanceOf(UserDetailsAdapter.class);
      verify(userRepositoryPort, times(1)).findById(user.getId());
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o usuário não for encontrado")
    void loadUserById_shouldThrowException_whenUserNotFound() {
      // Arrange
      UUID userId = UUID.randomUUID();
      when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userDetailsService.loadUserById(userId))
          .isInstanceOf(UsernameNotFoundException.class)
          .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
    @DisplayName(
        "Deve lançar AccountStatusAuthenticationException quando o status da conta não for ACTIVE")
    void loadUserById_shouldThrowAccountStatusException_whenAccountNotActive(
        AccountStatus status, ErrorCode expectedError) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(status).build();

      when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> userDetailsService.loadUserById(user.getId()))
          .isInstanceOf(AccountStatusAuthenticationException.class)
          .satisfies(
              ex -> {
                AccountStatusAuthenticationException exception =
                    (AccountStatusAuthenticationException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(expectedError);
              });
    }
  }
}
