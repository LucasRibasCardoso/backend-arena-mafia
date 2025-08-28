package com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.imp;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.ForgotPasswordUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ForgotPasswordUseCaseImp implements ForgotPasswordUseCase {

  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;
  private final ApplicationEventPublisher eventPublisher;

  public ForgotPasswordUseCaseImp(
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void execute(ForgotPasswordRequestDto requestDto) {
    String formattedPhone = phoneValidator.formatToE164(requestDto.phone());
    userRepository
        .findByPhone(formattedPhone)
        .ifPresent(
            user -> {
              eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
            });
  }
}
