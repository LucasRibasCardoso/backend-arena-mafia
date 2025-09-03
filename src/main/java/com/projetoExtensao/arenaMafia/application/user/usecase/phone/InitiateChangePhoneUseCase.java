package com.projetoExtensao.arenaMafia.application.user.usecase.phone;

import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePhoneRequestDTO;
import java.util.UUID;

public interface InitiateChangePhoneUseCase {
  void execute(UUID idCurrentUser, ChangePhoneRequestDTO request);
}
