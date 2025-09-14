package com.projetoExtensao.arenaMafia.application.user.usecase.password.imp;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.password.ChangePasswordUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.IncorrectCurrentPasswordException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePasswordRequestDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangePasswordUseCaseImp implements ChangePasswordUseCase {

  private final PasswordEncoderPort passwordEncoder;
  private final UserRepositoryPort userRepository;

  public ChangePasswordUseCaseImp(
      PasswordEncoderPort passwordEncoder, UserRepositoryPort userRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
  }

  @Override
  public void execute(UUID idCurrentUser, ChangePasswordRequestDto request) {
    User user = getUserOrElseThrow(idCurrentUser);
    verifyCurrentPassword(request.currentPassword(), user.getPasswordHash());

    String newPasswordHash = passwordEncoder.encode(request.newPassword());
    user.updatePasswordHash(newPasswordHash);
    userRepository.save(user);
  }

  private User getUserOrElseThrow(UUID idCurrentUser) {
    return userRepository.findById(idCurrentUser).orElseThrow(UserNotFoundException::new);
  }

  private void verifyCurrentPassword(String rawPassword, String encodedPassword) {
    if (!isPasswordSameAsCurrent(rawPassword, encodedPassword)) {
      throw new IncorrectCurrentPasswordException();
    }
  }

  private boolean isPasswordSameAsCurrent(String newPassword, String currentPassword) {
    return passwordEncoder.matches(newPassword, currentPassword);
  }
}
