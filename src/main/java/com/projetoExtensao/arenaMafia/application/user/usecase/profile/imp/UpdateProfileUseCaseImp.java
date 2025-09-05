package com.projetoExtensao.arenaMafia.application.user.usecase.profile.imp;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.UpdateProfileUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDTO;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateProfileUseCaseImp implements UpdateProfileUseCase {

  private final UserRepositoryPort userRepository;

  public UpdateProfileUseCaseImp(UserRepositoryPort userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User execute(UUID idCurrentUser, UpdateProfileRequestDTO request) {
    User user = getUserOrElseThrow(idCurrentUser);
    user.updateFullName(request.fullName());
    return userRepository.save(user);
  }

  private User getUserOrElseThrow(UUID idCurrentUser) {
    return userRepository
        .findById(idCurrentUser)
        .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
  }
}
