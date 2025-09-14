package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ResetPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordResetTokenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
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
  public void execute(ResetPasswordRequestDto request) {
    ResetToken resetToken = request.passwordResetToken();
    UUID userId = getUserIdFromToken(resetToken);
    User user = getUserById(userId);
    user.ensureAccountEnabled();

    String newPasswordHash = passwordEncoder.encode(request.newPassword());
    user.updatePasswordHash(newPasswordHash);
    userRepositoryPort.save(user);

    passwordResetTokenPort.delete(resetToken);
  }

  private UUID getUserIdFromToken(ResetToken token) {
    return passwordResetTokenPort
        .findUserIdByResetToken(token)
        .orElseThrow(InvalidPasswordResetTokenException::new);
  }

  private User getUserById(UUID userId) {
    return userRepositoryPort.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
