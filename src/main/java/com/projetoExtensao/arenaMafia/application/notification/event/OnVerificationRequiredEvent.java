package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.User;

public record OnVerificationRequiredEvent(User user) {}
