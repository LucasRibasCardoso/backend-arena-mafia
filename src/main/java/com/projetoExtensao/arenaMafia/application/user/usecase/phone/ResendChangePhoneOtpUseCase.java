package com.projetoExtensao.arenaMafia.application.user.usecase.phone;

import java.util.UUID;

public interface ResendChangePhoneOtpUseCase {
  void execute(UUID idCurrentUser);
}
