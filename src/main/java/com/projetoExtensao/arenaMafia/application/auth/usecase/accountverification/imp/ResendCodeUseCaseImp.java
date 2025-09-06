package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp;

import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.ResendCodeUseCase;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.valueobjects.UserId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResendCodeUseCaseImp implements ResendCodeUseCase {

  private final UserRepositoryPort userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ResendCodeUseCaseImp(
      UserRepositoryPort userRepository, ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(ResendCodeRequestDto requestDto) {
    UserId userId = UserId.fromString(requestDto.userId());
    userRepository
        .findById(userId.value())
        .ifPresent(
            user -> {
              user.ensurePendingVerification();
              eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
            });
  }
}
