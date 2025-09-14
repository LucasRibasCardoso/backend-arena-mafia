package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.SignUpUseCase;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignUpUseCaseImp implements SignUpUseCase {

  private final OtpSessionPort otpSessionPort;
  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;
  private final PasswordEncoderPort passwordEncoderPort;
  private final ApplicationEventPublisher eventPublisher;

  public SignUpUseCaseImp(
      OtpSessionPort otpSessionPort,
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      PasswordEncoderPort passwordEncoderPort,
      ApplicationEventPublisher eventPublisher) {
    this.otpSessionPort = otpSessionPort;
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.eventPublisher = eventPublisher;
    this.passwordEncoderPort = passwordEncoderPort;
  }

  @Override
  public OtpSessionId execute(SignupRequestDto request) {
    String formattedPhone = phoneValidator.formatToE164(request.phone());
    validateUniqueness(request.username(), formattedPhone);

    User userToSave = createNewUser(request, formattedPhone);
    User savedUser = userRepository.save(userToSave);

    OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(savedUser.getId());
    eventPublisher.publishEvent(new OnVerificationRequiredEvent(savedUser));
    return otpSessionId;
  }

  private void validateUniqueness(String username, String phone) {
    if (userRepository.existsByUsername(username)) {
      throw new UserAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    if (userRepository.existsByPhone(phone)) {
      throw new UserAlreadyExistsException(ErrorCode.PHONE_ALREADY_EXISTS);
    }
  }

  private User createNewUser(SignupRequestDto request, String formattedPhone) {
    String passwordHash = passwordEncoderPort.encode(request.password());
    return User.create(request.username(), request.fullName(), formattedPhone, passwordHash);
  }
}
