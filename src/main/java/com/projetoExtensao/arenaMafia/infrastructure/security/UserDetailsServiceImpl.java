package com.projetoExtensao.arenaMafia.infrastructure.security;

import com.projetoExtensao.arenaMafia.application.repository.UserRepositoryPort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

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
            () -> new UsernameNotFoundException("User not found with username: " + username));
  }
}
