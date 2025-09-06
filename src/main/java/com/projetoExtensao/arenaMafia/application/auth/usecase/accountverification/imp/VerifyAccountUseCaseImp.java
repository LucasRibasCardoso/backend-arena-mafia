package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.VerifyAccountUseCase;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.UserId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VerifyAccountUseCaseImp implements VerifyAccountUseCase {

  private final AuthPort authPort;
  private final OtpPort otpPort;
  private final UserRepositoryPort userRepository;

  public VerifyAccountUseCaseImp(
      AuthPort authPort, OtpPort otpPort, UserRepositoryPort userRepository) {
    this.otpPort = otpPort;
    this.authPort = authPort;
    this.userRepository = userRepository;
  }

  @Override
  public AuthResult execute(ValidateOtpRequestDto request) {
    UserId userId = UserId.fromString(request.userId());
    User user = getUserByIdOrElseThrow(userId.value());
    otpPort.validateOtp(user.getId(), request.code());
    user.confirmVerification();
    userRepository.save(user);
    return authPort.generateTokens(user);
  }

  private User getUserByIdOrElseThrow(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () ->
                new UserNotFoundException(
                    "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta."));
  }
}
