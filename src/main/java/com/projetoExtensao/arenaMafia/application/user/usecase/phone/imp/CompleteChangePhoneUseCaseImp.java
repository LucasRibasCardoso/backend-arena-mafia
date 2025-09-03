package com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.CompleteChangePhoneUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPhoneChangeRequestException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.CompletePhoneChangeRequestDTO;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompleteChangePhoneUseCaseImp implements CompleteChangePhoneUseCase {

  private final OtpPort otpPort;
  private final UserRepositoryPort userRepositoryPort;
  private final PendingPhoneChangePort pendingPhoneChangePort;

  public CompleteChangePhoneUseCaseImp(
      OtpPort otpPort,
      UserRepositoryPort userRepositoryPort,
      PendingPhoneChangePort pendingPhoneChangePort) {
    this.otpPort = otpPort;
    this.userRepositoryPort = userRepositoryPort;
    this.pendingPhoneChangePort = pendingPhoneChangePort;
  }

  @Override
  public User execute(UUID idCurrentUser, CompletePhoneChangeRequestDTO request) {
    String newPhone = getPhoneOrElseThrow(idCurrentUser);
    otpPort.validateOtp(idCurrentUser, request.code());
    pendingPhoneChangePort.deleteByUserId(idCurrentUser);

    User user = getUserOrElseThrow(idCurrentUser);
    user.updatePhone(newPhone);
    return userRepositoryPort.save(user);
  }

  private String getPhoneOrElseThrow(UUID idCurrentUser) {
    return pendingPhoneChangePort
        .findPhoneByUserId(idCurrentUser)
        .orElseThrow(
            () ->
                new InvalidPhoneChangeRequestException(
                    "Sua solicitação de alteração de telefone já expirou. Tente novamente."));
  }

  private User getUserOrElseThrow(UUID idCurrentUser) {
    return userRepositoryPort
        .findById(idCurrentUser)
        .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
  }
}
