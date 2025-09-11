package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultPassword;
import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultUsername;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para UserDetailsAdapter")
public class UserDetailsAdapterTest {

  @Test
  @DisplayName("Deve mapear corretamente as propriedades do User para a interface UserDetails")
  void shouldCorrectlyMapUserPropertiesToUserDetails() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UserDetailsAdapter userDetailsAdapter = new UserDetailsAdapter(user);

    // Act & Assert
    assertThat(userDetailsAdapter.getUsername()).isEqualTo(defaultUsername);
    assertThat(userDetailsAdapter.getPassword()).isEqualTo(defaultPassword);
    assertThat(userDetailsAdapter.isEnabled()).isTrue();
    assertThat(userDetailsAdapter.isAccountNonLocked()).isTrue();

    // Verifica a lógica de transformação de 'role'
    assertThat(userDetailsAdapter.getAuthorities()).hasSize(1);
    GrantedAuthority authority = userDetailsAdapter.getAuthorities().iterator().next();
    assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");

    // Verifica a lógica de atributos configurados hardcoded
    assertThat(userDetailsAdapter.isAccountNonExpired()).isTrue();
    assertThat(userDetailsAdapter.isCredentialsNonExpired()).isTrue();
  }
}
