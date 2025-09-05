package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp;

import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.SignUpUseCase;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignUpUseCaseImp implements SignUpUseCase {

  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;
  private final PasswordEncoderPort passwordEncoderPort;
  private final ApplicationEventPublisher eventPublisher;

  public SignUpUseCaseImp(
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      PasswordEncoderPort passwordEncoderPort,
      ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.passwordEncoderPort = passwordEncoderPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public String execute(SignupRequestDto requestDto) {
    String formattedPhone = phoneValidator.formatToE164(requestDto.phone());

    validateUniqueness(requestDto.username(), formattedPhone);

    String encodedPassword = passwordEncoderPort.encode(requestDto.password());

    User userToSave =
        User.create(requestDto.username(), requestDto.fullName(), formattedPhone, encodedPassword);

    User savedUser = userRepository.save(userToSave);
    eventPublisher.publishEvent(new OnVerificationRequiredEvent(savedUser));
    return savedUser.getPhone();
  }

  private void validateUniqueness(String username, String phone) {
    if (userRepository.existsByUsername(username)) {
      throw new UserAlreadyExistsException("Esse nome de usuário já está em uso.");
    }
    if (userRepository.existsByPhone(phone)) {
      throw new UserAlreadyExistsException("Esse número de telefone já está em uso.");
    }
  }
}
