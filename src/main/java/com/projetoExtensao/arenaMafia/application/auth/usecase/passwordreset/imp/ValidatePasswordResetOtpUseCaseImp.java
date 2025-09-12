package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ValidatePasswordResetOtpUseCase;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpSessionException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ValidatePasswordResetOtpUseCaseImp implements ValidatePasswordResetOtpUseCase {

  private final OtpPort otpPort;
  private final OtpSessionPort otpSessionPort;
  private final UserRepositoryPort userRepository;
  private final PasswordResetTokenPort passwordResetToken;

  public ValidatePasswordResetOtpUseCaseImp(
      OtpPort otpPort,
      OtpSessionPort otpSessionPort,
      UserRepositoryPort userRepository,
      PasswordResetTokenPort passwordResetToken) {
    this.otpPort = otpPort;
    this.otpSessionPort = otpSessionPort;
    this.userRepository = userRepository;
    this.passwordResetToken = passwordResetToken;
  }

  @Override
  public PasswordResetTokenResponseDto execute(ValidateOtpRequestDto request) {
    UUID userId = getUserIdFromOtpSession(request.otpSessionId());
    User user = getUserById(userId);

    user.ensureAccountEnabled();
    otpPort.validateOtp(user.getId(), request.otpCode());
    ResetToken token = passwordResetToken.generateToken(user.getId());
    return new PasswordResetTokenResponseDto(token);
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
