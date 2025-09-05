package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.VerifyAccountUseCase;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VerifyAccountUseCaseImp implements VerifyAccountUseCase {

  private final AuthPort authPort;
  private final OtpPort otpPort;
  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;

  public VerifyAccountUseCaseImp(
      AuthPort authPort,
      OtpPort otpPort,
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator) {
    this.otpPort = otpPort;
    this.authPort = authPort;
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
  }

  @Override
  public AuthResult execute(ValidateOtpRequestDto requestDto) {
    String formattedPhone = phoneValidator.formatToE164(requestDto.phone());
    User user = getUserByPhoneOrElseThrow(formattedPhone);
    otpPort.validateOtp(user.getId(), requestDto.code());
    user.confirmVerification();
    userRepository.save(user);
    return authPort.generateTokens(user);
  }

  private User getUserByPhoneOrElseThrow(String phone) {
    return userRepository
        .findByPhone(phone)
        .orElseThrow(
            () ->
                new UserNotFoundException(
                    "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta."));
  }
}
