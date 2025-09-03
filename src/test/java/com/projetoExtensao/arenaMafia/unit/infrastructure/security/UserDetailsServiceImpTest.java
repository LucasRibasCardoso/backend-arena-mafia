package com.projetoExtensao.arenaMafia.unit.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
      String username = "testuser";
      String password = "passwordHash";
      User mockUser = User.create(username, "Test User", "+5547912345678", password);

      when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.of(mockUser));

      // Act
      UserDetails userDetails = userDetailsService.loadUserByUsername(username);

      // Assert
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(username);
      assertThat(userDetails.getPassword()).isEqualTo(password);
      assertThat(userDetails).isInstanceOf(UserDetailsAdapter.class);
      verify(userRepositoryPort, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o usuário não for encontrado")
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
      // Arrange
      String username = "usernotfound";
      when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
          .isInstanceOf(UsernameNotFoundException.class)
          .hasMessage("Usuário com o nome '" + username + "' não foi encontrado.");
    }
  }

  @Nested
  @DisplayName("Testes para o método loadUserById")
  class LoadUserByIdTests {

    @Test
    @DisplayName("Deve retornar UserDetails quando o usuário for encontrado pelo ID")
    void loadUserById_shouldReturnUserDetails_whenUserExists() {
      // Arrange
      String username = "testuser";
      String password = "passwordHash";
      User mockUser = User.create(username, "Test User", "+5547912345678", password);
      UUID userId = UUID.randomUUID();

      when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(mockUser));

      // Act
      UserDetails userDetails = userDetailsService.loadUserById(userId);

      // Assert
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(username);
      assertThat(userDetails.getPassword()).isEqualTo(password);
      assertThat(userDetails).isInstanceOf(UserDetailsAdapter.class);
      verify(userRepositoryPort, times(1)).findById(userId);
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
          .hasMessage(
              "Usuário com o ID '" + userId + "' referenciado no token JWT não foi encontrado.");
    }
  }
}
