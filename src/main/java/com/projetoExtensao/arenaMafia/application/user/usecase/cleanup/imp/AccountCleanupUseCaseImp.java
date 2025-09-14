package com.projetoExtensao.arenaMafia.application.user.usecase.cleanup.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.cleanup.AccountCleanupUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountCleanupUseCaseImp implements AccountCleanupUseCase {

  private static final Logger logger = LoggerFactory.getLogger(AccountCleanupUseCaseImp.class);

  private final UserRepositoryPort userRepository;
  private final RefreshTokenRepositoryPort refreshTokenRepository;

  public AccountCleanupUseCaseImp(
      UserRepositoryPort userRepository, RefreshTokenRepositoryPort refreshTokenRepository) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Transactional
  @Scheduled(cron = "0 0 3 * * ?", zone = "America/Sao_Paulo") // 3h no fuso de São Paulo
  @Override
  public void executeCleanupOfPendingAccounts() {
    logger.info("INICIANDO TAREFA AGENDADA: Limpeza de contas pendentes.");
    Instant limitDate = Instant.now().minus(24, ChronoUnit.HOURS); // 1 dia atrás

    List<User> accountsToClean =
        userRepository.findByStatusAndCreatedAtBefore(
            AccountStatus.PENDING_VERIFICATION, limitDate);

    if (accountsToClean.isEmpty()) {
      logger.info("Nenhuma conta pendente foi encontrada para limpar. Tarefa concluída.");
      return;
    }
    logger.info("Contas pendentes para exclusão permanente: {}", accountsToClean.size());
    refreshTokenRepository.deleteAllByUser(accountsToClean);
    userRepository.deleteAll(accountsToClean);
    logger.info("TAREFA FINALIZADA: {} contas pendentes foram excluídas.", accountsToClean.size());
  }

  @Transactional
  @Scheduled(cron = "0 0 4 * * ?", zone = "America/Sao_Paulo") // 4h no fuso de São Paulo
  @Override
  public void executeCleanupOfDisabledAccounts() {
    logger.info("INICIANDO TAREFA AGENDADA: Limpeza de contas desativadas.");
    Instant limitDate = Instant.now().minus(7, ChronoUnit.DAYS); // 7 dia atrás

    List<User> accountsToClean =
        userRepository.findByStatusAndUpdateAtBefore(AccountStatus.DISABLED, limitDate);

    if (accountsToClean.isEmpty()) {
      logger.info("Nenhuma conta desativada foi encontrada para limpar. Tarefa concluída.");
      return;
    }
    logger.info("Contas desativadas para exclusão permanente: {}", accountsToClean.size());
    refreshTokenRepository.deleteAllByUser(accountsToClean);
    userRepository.deleteAll(accountsToClean);
    logger.info(
        "TAREFA FINALIZADA: {} contas desativadas foram excluídas.", accountsToClean.size());
  }
}
