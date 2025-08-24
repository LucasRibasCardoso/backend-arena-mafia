package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler.utilsExceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenInvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.user.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.AccountLockedException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.AccountNotVerifiedException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.DisabledAccountException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/exceptions/auth/")
public class TestAuthExceptionHandlerController {

  @GetMapping("/refresh-token-expired")
  public void throwBadRefreshToken() {
    throw new RefreshTokenExpiredException("Refresh token expirado");
  }

  @GetMapping("/refresh-not-found")
  public void throwNotFoundRefreshToken() {
    throw new RefreshTokenNotFoundException("Refresh token expirado");
  }

  @GetMapping("/invalid-format-refresh-token")
  public void throwInvalidFormatRefreshToken() {
    throw new RefreshTokenInvalidFormatException("Formato inválido do refresh token");
  }

  @GetMapping("/bad-phone")
  public void throwBadPhone() {
    throw new BadPhoneNumberException("Telefone inválido");
  }

  @GetMapping("/access-denied")
  public void throwAccessDenied() {
    throw new AccessDeniedException("Acesso negado para acessar esse recurso");
  }

  @GetMapping("/badCredentials")
  public void throwBadCredentials() {
    throw new BadCredentialsException("Credenciais inválidas");
  }

  @GetMapping("/disabled-account")
  public void throwDisabledAccount() {
    throw new DisabledAccountException("Está conta não está ativa.");
  }

  @GetMapping("/account-locked")
  public void throwAccountLocked() {
    throw new AccountLockedException("Sua conta está bloqueada. Por favor, contate o suporte.");
  }

  @GetMapping("/account-not-verified")
  public void throwAccountNotVerified() {
    throw new AccountNotVerifiedException("Sua conta ainda não foi verificada.");
  }

  @GetMapping("/bad-phone-number")
  public void throwBadPhoneNumber() {
    throw new BadPhoneNumberException("Número de telefone inválido");
  }
}
