package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.GeneratePasswordResetTokenUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GeneratePasswordResetTokenUseCaseImp implements GeneratePasswordResetTokenUseCase {

  private final OtpPort otpPort;
  private final PhoneValidatorPort phoneValidator;
  private final UserRepositoryPort userRepository;
  private final PasswordResetTokenPort passwordResetToken;

  public GeneratePasswordResetTokenUseCaseImp(
      OtpPort otpPort,
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      PasswordResetTokenPort passwordResetToken) {
    this.otpPort = otpPort;
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.passwordResetToken = passwordResetToken;
  }

  @Override
  public String execute(ValidateOtpRequestDto requestDto) {
    String formattedPhone = phoneValidator.formatToE164(requestDto.phone());
    User user = findUserByPhone(formattedPhone);
    user.ensureAccountEnabled();
    otpPort.validateOtp(user.getId(), requestDto.code());
    return passwordResetToken.save(user.getId());
  }

  private User findUserByPhone(String phone) {
    return userRepository
        .findByPhone(phone)
        .orElseThrow(
            () ->
                new UserNotFoundException(
                    "Usuário não encontrado. Verifique o número de telefone informado e tente novamente."));
  }
}
