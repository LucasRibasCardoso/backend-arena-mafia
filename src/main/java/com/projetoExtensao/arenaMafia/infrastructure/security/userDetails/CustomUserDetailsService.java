package com.projetoExtensao.arenaMafia.infrastructure.security.userDetails;

import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface CustomUserDetailsService extends UserDetailsService {
  UserDetails loadUserById(UUID id) throws UsernameNotFoundException;
}
