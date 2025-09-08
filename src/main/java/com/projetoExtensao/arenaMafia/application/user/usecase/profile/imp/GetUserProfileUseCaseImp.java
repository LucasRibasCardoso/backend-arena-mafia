package com.projetoExtensao.arenaMafia.application.user.usecase.profile.imp;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.GetUserProfileUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetUserProfileUseCaseImp implements GetUserProfileUseCase {

  private final UserRepositoryPort userRepository;

  public GetUserProfileUseCaseImp(UserRepositoryPort userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User execute(UUID userId) {
    return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
  }
}
