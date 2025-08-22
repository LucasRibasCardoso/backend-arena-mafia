package com.projetoExtensao.arenaMafia.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.projetoExtensao.arenaMafia.application.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.security.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.UserDetailsServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para UserDetailsServiceImp")
public class UserDetailsServiceImpTest {

  @Mock private UserRepositoryPort userRepositoryPort;

  @InjectMocks private UserDetailsServiceImpl userDetailsService;

  @Test
  @DisplayName("Deve retornar UserDetails quando o usuário for encontrado")
  void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {
    // Arrange
    String username = "testuser";
    String password = "passwordHash";
    User mockUser = User.create(username, "Test User", "5547912345678", password);

    when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.of(mockUser));

    // Act
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // Assert
    assertThat(userDetails).isNotNull();
    assertThat(userDetails.getUsername()).isEqualTo(username);
    assertThat(userDetails.getPassword()).isEqualTo(password);
    assertThat(userDetails).isInstanceOf(UserDetailsAdapter.class);
  }

  @Test
  @DisplayName("Deve lançar UsernameNotFoundException quando o usuário não for encontrado")
  void loadUserByUsername_shouldThrowExceptionWhenUserNotFound() {
    // Arrange
    String username = "usernotfound";
    when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found with username: " + username);
  }
}
