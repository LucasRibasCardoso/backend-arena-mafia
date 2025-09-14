package com.projetoExtensao.arenaMafia.application.auth.usecase.otp.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.otp.ResendOtpUseCase;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpSessionException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResendOtpUseCaseImp implements ResendOtpUseCase {

  private final ApplicationEventPublisher eventPublisher;
  private final OtpSessionPort otpSessionPort;
  private final UserRepositoryPort userRepository;

  public ResendOtpUseCaseImp(
      ApplicationEventPublisher eventPublisher,
      OtpSessionPort otpSessionPort,
      UserRepositoryPort userRepository) {
    this.eventPublisher = eventPublisher;
    this.otpSessionPort = otpSessionPort;
    this.userRepository = userRepository;
  }

  @Override
  public void execute(OtpSessionId otpSessionId) {
    UUID userId = getUserIdFromOtpSession(otpSessionId);
    User user = getUserById(userId);

    user.ensureCanRequestOtp();
    eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
  }

  private UUID getUserIdFromOtpSession(OtpSessionId otpSessionId) {
    return otpSessionPort
        .findUserIdByOtpSessionId(otpSessionId)
        .orElseThrow(InvalidOtpSessionException::new);
  }

  private User getUserById(UUID userId) {
    return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
