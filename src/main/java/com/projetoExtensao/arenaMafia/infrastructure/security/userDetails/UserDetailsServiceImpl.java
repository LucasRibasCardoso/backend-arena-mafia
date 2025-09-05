package com.projetoExtensao.arenaMafia.infrastructure.security.userDetails;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements CustomUserDetailsService {

  private final UserRepositoryPort userRepositoryPort;

  public UserDetailsServiceImpl(UserRepositoryPort userRepositoryPort) {
    this.userRepositoryPort = userRepositoryPort;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepositoryPort
        .findByUsername(username)
        .map(UserDetailsAdapter::new)
        .orElseThrow(
            () ->
                new UsernameNotFoundException(
                    "Usuário com o nome '" + username + "' não foi encontrado."));
  }

  @Override
  public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
    return userRepositoryPort
        .findById(id)
        .map(UserDetailsAdapter::new)
        .orElseThrow(
            () ->
                new UsernameNotFoundException(
                    "Usuário com o ID '" + id + "' referenciado no token JWT não foi encontrado."));
  }
}
