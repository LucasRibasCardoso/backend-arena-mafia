package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.VerifyAccountUseCase;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.InvalidOtpSessionException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VerifyAccountUseCaseImp implements VerifyAccountUseCase {

  private final OtpPort otpPort;
  private final AuthPort authPort;
  private final OtpSessionPort otpSessionPort;
  private final UserRepositoryPort userRepository;

  public VerifyAccountUseCaseImp(
      OtpPort otpPort,
      AuthPort authPort,
      OtpSessionPort otpSessionPort,
      UserRepositoryPort userRepository) {
    this.otpPort = otpPort;
    this.authPort = authPort;
    this.otpSessionPort = otpSessionPort;
    this.userRepository = userRepository;
  }

  @Override
  public AuthResult execute(ValidateOtpRequestDto request) {
    UUID userId = getUserIdFromOtpSession(request.otpSessionId());
    User user = getUserById(userId);

    otpPort.validateOtp(user.getId(), request.otpCode());
    user.confirmVerification();
    userRepository.save(user);

    return authPort.generateTokens(user);
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
