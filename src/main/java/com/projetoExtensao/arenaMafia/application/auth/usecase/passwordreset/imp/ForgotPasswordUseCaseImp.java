package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ForgotPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ForgotPasswordUseCaseImp implements ForgotPasswordUseCase {

  private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordUseCaseImp.class);

  private final OtpSessionPort otpSessionPort;
  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;
  private final ApplicationEventPublisher eventPublisher;

  public ForgotPasswordUseCaseImp(
      OtpSessionPort otpSessionPort,
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      ApplicationEventPublisher eventPublisher) {
    this.otpSessionPort = otpSessionPort;
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public ForgotPasswordResponseDto execute(ForgotPasswordRequestDto request) {
    String formattedPhone = phoneValidator.formatToE164(request.phone());
    String message = "Se o número estiver cadastrado, você receberá um código de verificação.";

    return userRepository
        .findByPhone(formattedPhone)
        .flatMap(this::generateSessionIfAccountIsEnabled)
        .map(otpSessionId -> new ForgotPasswordResponseDto(otpSessionId, message))
        .orElseGet(() -> generateSecureFallbackResponse(message));
  }

  private Optional<String> generateSessionIfAccountIsEnabled(User user) {
    try {
      user.ensureAccountEnabled();
      String otpSessionId = otpSessionPort.generateOtpSession(user.getId());
      eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
      return Optional.of(otpSessionId);
    } catch (AccountStatusException e) {
      logger.warn("Tentativa de redefinição de senha para conta desabilitada: {}  ", user.getId());
      return Optional.empty();
    }
  }

  private ForgotPasswordResponseDto generateSecureFallbackResponse(String message) {
    String fakeOtpSessionId = UUID.randomUUID().toString();
    return new ForgotPasswordResponseDto(fakeOtpSessionId, message);
  }
}
