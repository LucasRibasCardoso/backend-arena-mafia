package com.projetoExtensao.arenaMafia.application.auth.usecase;

public interface LogoutUseCase {

  void execute(String refreshToken);
}
