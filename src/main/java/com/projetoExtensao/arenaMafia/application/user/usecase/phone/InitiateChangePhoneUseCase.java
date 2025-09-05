package com.projetoExtensao.arenaMafia.application.user.usecase.phone;

import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.InitiateChangePhoneRequestDTO;
import java.util.UUID;

public interface InitiateChangePhoneUseCase {
  void execute(UUID idCurrentUser, InitiateChangePhoneRequestDTO request);
}
