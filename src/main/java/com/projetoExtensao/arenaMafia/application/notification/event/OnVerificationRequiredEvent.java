package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.Optional;

public class OnVerificationRequiredEvent {

  private final User user;
  private final String targetPhone;

  public OnVerificationRequiredEvent(User user) {
    this.user = user;
    this.targetPhone = null;
  }

  public OnVerificationRequiredEvent(User user, String targetPhone) {
    this.user = user;
    this.targetPhone = targetPhone;
  }

  public User getUser() {
    return user;
  }

  public String getRecipientPhone() {
    return Optional.ofNullable(targetPhone).orElse(user.getPhone());
  }
}
