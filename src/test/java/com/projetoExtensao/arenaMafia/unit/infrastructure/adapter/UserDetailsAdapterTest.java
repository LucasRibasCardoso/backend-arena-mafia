package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para UserDetailsAdapter")
public class UserDetailsAdapterTest {

  @Mock private User mockUser;

  private UserDetailsAdapter userDetailsAdapter;

  @BeforeEach
  public void setUp() {
    // Criar a issntância do adapter antes de cada teste
    userDetailsAdapter = new UserDetailsAdapter(mockUser);
  }

  @Test
  @DisplayName("Deve mapear corretamente as propriedades do User para a interface UserDetails")
  void shouldCorrectlyMapUserPropertiesToUserDetails() {
    // Arrange
    when(mockUser.getUsername()).thenReturn("testuser");
    when(mockUser.getPasswordHash()).thenReturn("hashedPassword123");
    when(mockUser.getRole()).thenReturn(RoleEnum.ROLE_ADMIN);
    when(mockUser.isEnabled()).thenReturn(true);
    when(mockUser.isAccountNonLocked()).thenReturn(true);

    // Act & Assert
    assertThat(userDetailsAdapter.getUsername()).isEqualTo("testuser");
    assertThat(userDetailsAdapter.getPassword()).isEqualTo("hashedPassword123");
    assertThat(userDetailsAdapter.isEnabled()).isTrue();
    assertThat(userDetailsAdapter.isAccountNonLocked()).isTrue();

    // Verifica a lógica de transformação de 'role'
    assertThat(userDetailsAdapter.getAuthorities()).hasSize(1);
    GrantedAuthority authority = userDetailsAdapter.getAuthorities().iterator().next();
    assertThat(authority.getAuthority()).isEqualTo("ROLE_ADMIN");

    // Verifica a lógica de atributos configurados hardcoded
    assertThat(userDetailsAdapter.isAccountNonExpired()).isTrue();
    assertThat(userDetailsAdapter.isCredentialsNonExpired()).isTrue();
  }
}
