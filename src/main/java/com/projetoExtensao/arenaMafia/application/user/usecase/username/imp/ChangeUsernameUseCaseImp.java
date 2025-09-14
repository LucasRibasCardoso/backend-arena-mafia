package com.projetoExtensao.arenaMafia.application.user.usecase.username.imp;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.username.ChangeUsernameUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangeUsernameRequestDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeUsernameUseCaseImp implements ChangeUsernameUseCase {

  private final UserRepositoryPort userRepository;

  public ChangeUsernameUseCaseImp(UserRepositoryPort userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User execute(UUID idCurrentUser, ChangeUsernameRequestDto request) {
    checkIfUsernameAlreadyExists(idCurrentUser, request.username());
    User user = getUserOrElseThrow(idCurrentUser);
    user.updateUsername(request.username());
    return userRepository.save(user);
  }

  private void checkIfUsernameAlreadyExists(UUID idCurrentUser, String username) {
    userRepository
        .findByUsername(username)
        .ifPresent(
            userFound -> {
              if (!userFound.getId().equals(idCurrentUser)) {
                throw new UserAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS);
              }
            });
  }

  private User getUserOrElseThrow(UUID idCurrentUser) {
    return userRepository.findById(idCurrentUser).orElseThrow(UserNotFoundException::new);
  }
}
