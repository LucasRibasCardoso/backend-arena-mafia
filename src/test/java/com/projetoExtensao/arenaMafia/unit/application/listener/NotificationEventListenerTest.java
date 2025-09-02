package com.projetoExtensao.arenaMafia.unit.application.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.notification.listener.NotificationEventListener;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para NotificationEventListener")
public class NotificationEventListenerTest {

  @Mock private SmsPort smsPort;
  @Mock private OtpPort otpPort;

  @InjectMocks private NotificationEventListener eventListener;

  @Test
  @DisplayName("Deve gerar OTP e enviar SMS quando um UserRegisteredEvent é recebido")
  void osUserRegistration_shouldGenerateOtpAndSendSms_onEvent() {
    // Arrange
    UUID userId = UUID.randomUUID();
    User user =
        User.reconstitute(
            userId,
            "testuser",
            "Test User",
            "+5511987654321",
            "hashedPassword",
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            Instant.now());
    OnVerificationRequiredEvent event = new OnVerificationRequiredEvent(user);

    String generatedOtp = "123456";

    when(otpPort.generateCodeOTP(userId)).thenReturn(generatedOtp);

    // Act
    eventListener.osUserRegistration(event);

    // Assert
    verify(otpPort, times(1)).generateCodeOTP(userId);

    ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(smsPort, times(1)).send(phoneCaptor.capture(), messageCaptor.capture());

    assertEquals(user.getPhone(), phoneCaptor.getValue());
    assertTrue(messageCaptor.getValue().contains(generatedOtp));
    assertTrue(
        messageCaptor.getValue().contains("Seu código de verificação para a Arena Máfia é:"));
  }

  @Test
  @DisplayName("Não deve enviar SMS se a geração de OTP falhar")
  void osUserRegistration_shouldNotSendSms_whenOtpGenerationFails() {
    // Arrange
    UUID userId = UUID.randomUUID();
    User user =
        User.reconstitute(
            userId,
            "testuser",
            "Test User",
            "+5511987654321",
            "hashedPassword",
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            Instant.now());
    OnVerificationRequiredEvent event = new OnVerificationRequiredEvent(user);

    when(otpPort.generateCodeOTP(user.getId()))
        .thenThrow(new RuntimeException("Falha ao conectar com o Redis"));

    // Act & Assert
    assertDoesNotThrow(() -> eventListener.osUserRegistration(event));

    verify(smsPort, never()).send(anyString(), anyString());
  }
}
