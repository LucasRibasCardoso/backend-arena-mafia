package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ForgotPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

  private Optional<OtpSessionId> generateSessionIfAccountIsEnabled(User user) {
    try {
      user.ensureAccountEnabled();
      OtpSessionId otpSessionId = otpSessionPort.generateOtpSession(user.getId());
      eventPublisher.publishEvent(new OnVerificationRequiredEvent(user));
      return Optional.of(otpSessionId);
    } catch (AccountStatusForbiddenException e) {
      logger.warn(
          "Tentativa de redefinição de senha para conta com status inválido: [{}] : [{}] : [{}]",
          user.getId(),
          e.getErrorCode(),
          e.getErrorCode().getMessage());
      return Optional.empty();
    }
  }

  private ForgotPasswordResponseDto generateSecureFallbackResponse(String message) {
    OtpSessionId fakeOtpSessionId = OtpSessionId.generate();
    return new ForgotPasswordResponseDto(fakeOtpSessionId, message);
  }
}
