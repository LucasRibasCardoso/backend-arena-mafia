package com.projetoExtensao.arenaMafia.application.user.usecase.phone;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.CompletePhoneChangeRequestDto;
import java.util.UUID;

public interface CompleteChangePhoneUseCase {
  User execute(UUID idCurrentUser, CompletePhoneChangeRequestDto request);
}
