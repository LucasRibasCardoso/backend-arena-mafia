package com.projetoExtensao.arenaMafia.application.notification.listener;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

  private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

  private final SmsPort smsPort;
  private final OtpPort otpPort;

  public NotificationEventListener(SmsPort smsPort, OtpPort otpPort) {
    this.smsPort = smsPort;
    this.otpPort = otpPort;
  }

  @Async
  @EventListener
  public void onUserRegistration(OnVerificationRequiredEvent event) {
    try {
      User user = event.getUser();
      String recipientPhone = event.getRecipientPhone();

      OtpCode otpCode = otpPort.generateOtpCode(user.getId());

      String message =
          String.format(
              "Seu código de verificação para a Arena Máfia é: %s. Não compartilhe este código.",
              otpCode);

      smsPort.send(recipientPhone, message);

      logger.info("SMS de verificação enviado para o usuário: {}", user.getUsername());

    } catch (Exception e) {
      logger.error("Falha ao processar o evento de registro do usuário: {}", e.getMessage(), e);
    }
  }
}
