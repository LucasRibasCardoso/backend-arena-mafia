package com.projetoExtensao.arenaMafia.application.auth.usecase.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.VerifyAccountUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.user.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.VerifyAccountRequestDto;
import java.util.UUID;

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
  public AuthResult execute(VerifyAccountRequestDto requestDto) {
    String username = requestDto.username();
    String code = requestDto.verificationCode();

    User user = getUserIfExists(username);
    validateOtpCode(user.getId(), code);

    user.activateAccount();
    userRepository.save(user);

    return authPort.generateTokens(user);
  }

  private User getUserIfExists(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () ->
                new UserNotFoundException(
                    "Usuário não encontrado para realizar verificação. Por favor faça o cadastro novamente."));
  }

  private void validateOtpCode(UUID userId, String code) {
    boolean isValid = otpPort.validateOtp(userId, code);
    if (!isValid) {
      throw new InvalidOtpException("Código de verificação inválido ou expirado.");
    }
  }
}
