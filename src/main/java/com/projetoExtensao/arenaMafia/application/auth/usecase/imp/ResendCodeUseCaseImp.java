package com.projetoExtensao.arenaMafia.application.auth.usecase.imp;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ResendCodeUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.ResendCodeRequestDto;
import org.springframework.context.ApplicationEventPublisher;

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
    User user = getUserIfExists(requestDto.username());
    user.checkIfPendingVerification();
    eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
  }

  private User getUserIfExists(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () ->
                new UserNotFoundException(
                    "Usuário não encontrado para reenviar o código. Por favor realize o cadastro novamente."));
  }
}
