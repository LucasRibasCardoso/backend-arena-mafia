package com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.InitiateChangePhoneUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.InitiateChangePhoneRequestDto;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InitiateChangePhoneUseCaseImp implements InitiateChangePhoneUseCase {

  private final PendingPhoneChangePort pendingPhoneChangePort;
  private final ApplicationEventPublisher eventPublisher;
  private final PhoneValidatorPort phoneValidatorPort;
  private final UserRepositoryPort userRepository;

  public InitiateChangePhoneUseCaseImp(
      PendingPhoneChangePort pendingPhoneChangePort,
      ApplicationEventPublisher eventPublisher,
      PhoneValidatorPort phoneValidatorPort,
      UserRepositoryPort userRepository) {
    this.pendingPhoneChangePort = pendingPhoneChangePort;
    this.eventPublisher = eventPublisher;
    this.phoneValidatorPort = phoneValidatorPort;
    this.userRepository = userRepository;
  }

  @Override
  public void execute(UUID idCurrentUser, InitiateChangePhoneRequestDto request) {
    String formattedPhone = phoneValidatorPort.formatToE164(request.newPhone());

    checkIfPhoneAlreadyExists(idCurrentUser, formattedPhone);
    User user = getUserOrElseThrow(idCurrentUser);

    pendingPhoneChangePort.save(user.getId(), formattedPhone);
    eventPublisher.publishEvent((new OnVerificationRequiredEvent(user)));
  }

  private User getUserOrElseThrow(UUID idCurrentUser) {
    return userRepository.findById(idCurrentUser).orElseThrow(UserNotFoundException::new);
  }

  private void checkIfPhoneAlreadyExists(UUID idCurrentUser, String newPhone) {
    userRepository
        .findByPhone(newPhone)
        .ifPresent(
            userFound -> {
              if (!userFound.getId().equals(idCurrentUser)) {
                throw new UserAlreadyExistsException(ErrorCode.PHONE_ALREADY_EXISTS);
              }
            });
  }
}
