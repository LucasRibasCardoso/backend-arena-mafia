package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

public interface LogoutUseCase {

  void execute(String refreshToken);
}
