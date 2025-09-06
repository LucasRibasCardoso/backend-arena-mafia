package com.projetoExtensao.arenaMafia.application.user.usecase.disable.imp;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.disable.DisableMyAccountUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableMyAccountUseCaseImp implements DisableMyAccountUseCase {

  private final UserRepositoryPort userRepository;

  public DisableMyAccountUseCaseImp(UserRepositoryPort userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public void execute(UUID idCurrentUser) {
    User user =
        userRepository
            .findById(idCurrentUser)
            .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    user.disableAccount();
    userRepository.save(user);
  }
}
