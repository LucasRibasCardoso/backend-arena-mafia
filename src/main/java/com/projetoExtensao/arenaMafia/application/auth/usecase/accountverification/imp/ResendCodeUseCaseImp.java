package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.ResendCodeUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResendCodeUseCaseImp implements ResendCodeUseCase {

  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;
  private final ApplicationEventPublisher eventPublisher;

  public ResendCodeUseCaseImp(
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(ResendCodeRequestDto requestDto) {
    String formattedPhone = phoneValidator.formatToE164(requestDto.phone());
    userRepository
        .findByPhone(formattedPhone)
        .ifPresent(
            user -> {
              user.ensurePendingVerification();
              eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
            });
  }
}
