package com.projetoExtensao.arenaMafia.infrastructure.security.userDetails;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.AccountStatusAuthenticationException;
import com.projetoExtensao.arenaMafia.domain.model.User;
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
    User user =
        userRepositoryPort
            .findByUsername(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("Usuário não encontrado: " + username));

    validateAccountStatus(user);
    return new UserDetailsAdapter(user);
  }

  @Override
  public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
    User user =
        userRepositoryPort
            .findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + id));

    validateAccountStatus(user);
    return new UserDetailsAdapter(user);
  }

  private void validateAccountStatus(User user) {
    switch (user.getStatus()) {
      case ACTIVE:
        break;
      case LOCKED:
        throw new AccountStatusAuthenticationException(ErrorCode.ACCOUNT_LOCKED);
      case PENDING_VERIFICATION:
        throw new AccountStatusAuthenticationException(ErrorCode.ACCOUNT_PENDING_VERIFICATION);
      case DISABLED:
      default:
        throw new AccountStatusAuthenticationException(ErrorCode.ACCOUNT_DISABLED);
    }
  }
}
