package com.projetoExtensao.arenaMafia.application.auth.event;

import com.projetoExtensao.arenaMafia.domain.model.User;

public record OnVerificationRequiredEvent(User user) {}
