package com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.ResetPasswordUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResetPasswordUseCaseImp implements ResetPasswordUseCase {

  private final PasswordEncoderPort passwordEncoder;
  private final UserRepositoryPort userRepositoryPort;
  private final PasswordResetTokenPort passwordResetTokenPort;

  public ResetPasswordUseCaseImp(
      PasswordEncoderPort passwordEncoder,
      UserRepositoryPort userRepositoryPort,
      PasswordResetTokenPort passwordResetTokenPort) {
    this.passwordEncoder = passwordEncoder;
    this.userRepositoryPort = userRepositoryPort;
    this.passwordResetTokenPort = passwordResetTokenPort;
  }

  @Override
  public void execute(ResetPasswordRequestDto requestDto) {
    String resetToken = requestDto.passwordResetToken();
    UUID userId = passwordResetTokenPort.getUserIdByTokenOrElseThrow(resetToken);
    passwordResetTokenPort.delete(requestDto.passwordResetToken());

    User user =
        userRepositoryPort
            .findById(userId)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Ocorreu um erro ao redefinir sua senha. Por favor, inicie o processo novamente."));

    String passwordHash = passwordEncoder.encode(requestDto.newPassword());
    user.updatePasswordHash(passwordHash);
    userRepositoryPort.save(user);
  }
}
