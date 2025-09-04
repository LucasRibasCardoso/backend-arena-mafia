package com.projetoExtensao.arenaMafia.application.user.usecase.password.imp;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.password.ChangePasswordUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.IncorrectPasswordException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePasswordRequestDTO;
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
  public void execute(UUID idCurrentUser, ChangePasswordRequestDTO request) {
    User user = getUserOrElseThrow(idCurrentUser);
    checkIfCurrentPasswordIsEqualExisting(request.currentPassword(), user.getPasswordHash());

    String newPasswordHash = passwordEncoder.encode(request.newPassword());
    user.updatePasswordHash(newPasswordHash);
    userRepository.save(user);
  }

  private User getUserOrElseThrow(UUID idCurrentUser) {
    return userRepository
        .findById(idCurrentUser)
        .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
  }

  private void checkIfCurrentPasswordIsEqualExisting(
      String currentPasswordRequest, String existingPassword) {
    if (!passwordEncoder.matches(currentPasswordRequest, existingPassword)) {
      throw new IncorrectPasswordException("A Senha atual está incorreta");
    }
  }
}
