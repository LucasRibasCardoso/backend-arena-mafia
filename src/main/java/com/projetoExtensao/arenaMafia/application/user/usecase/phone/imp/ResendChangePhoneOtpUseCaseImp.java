package com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.ResendChangePhoneOtpUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PhoneChangeNotInitiatedException;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResendChangePhoneOtpUseCaseImp implements ResendChangePhoneOtpUseCase {

  private final UserRepositoryPort userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final PendingPhoneChangePort pendingPhoneChangePort;

  public ResendChangePhoneOtpUseCaseImp(
      UserRepositoryPort userRepository,
      ApplicationEventPublisher eventPublisher,
      PendingPhoneChangePort pendingPhoneChangePort) {
    this.userRepository = userRepository;
    this.eventPublisher = eventPublisher;
    this.pendingPhoneChangePort = pendingPhoneChangePort;
  }

  @Override
  public void execute(UUID idCurrentUser) {
    String newPhone = getNewPhoneFromCache(idCurrentUser);
    userRepository
        .findById(idCurrentUser)
        .ifPresent(
            user -> {
              user.ensureAccountEnabled();
              eventPublisher.publishEvent(new OnVerificationRequiredEvent(user, newPhone));
            });
  }

  private String getNewPhoneFromCache(UUID userId) {
    return pendingPhoneChangePort
        .findPhoneByUserId(userId)
        .orElseThrow(
            () ->
                new PhoneChangeNotInitiatedException(
                    "Nenhuma alteração de telefone pendente encontrada para o usuário."));
  }
}
