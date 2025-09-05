package com.projetoExtensao.arenaMafia.application.user.usecase.cleanup;

public interface AccountCleanupUseCase {

  void executeCleanupOfPendingAccounts();

  void executeCleanupOfDisabledAccounts();
}
