package com.projetoExtensao.arenaMafia.unit.application.notification.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.application.notification.listener.NotificationEventListener;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
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

  private final OtpCode otpCode = OtpCode.generate();

  private User createUser() {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+5511987654321",
        "hashedPassword",
        AccountStatus.ACTIVE,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve gerar OTP e enviar SMS para o número atual do usuário")
  void onUserRegistration_shouldGenerateOtpAndSendSms_onEvent() {
    // Arrange
    User user = createUser();
    UUID userId = user.getId();
    OnVerificationRequiredEvent event = new OnVerificationRequiredEvent(user);

    when(otpPort.generateOtpCode(userId)).thenReturn(otpCode);

    // Act
    eventListener.onUserRegistration(event);

    // Assert
    verify(otpPort, times(1)).generateOtpCode(userId);

    ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(smsPort, times(1)).send(phoneCaptor.capture(), messageCaptor.capture());

    assertEquals(user.getPhone(), phoneCaptor.getValue());
    assertTrue(messageCaptor.getValue().contains(otpCode.toString()));
    assertTrue(
        messageCaptor.getValue().contains("Seu código de verificação para a Arena Máfia é:"));
  }

  @Test
  @DisplayName("Deve gerar o OTP e enviar SMS para o novo número de telefone do usuário")
  void onUserRegistration_shouldGenerateOtpAndSendSmsOnEvent() {
    // Arrange
    User user = createUser();
    UUID userId = user.getId();
    String newPhone = "+5511999999999";
    OnVerificationRequiredEvent event = new OnVerificationRequiredEvent(user, newPhone);

    when(otpPort.generateOtpCode(userId)).thenReturn(otpCode);

    // Act
    eventListener.onUserRegistration(event);

    verify(otpPort, times(1)).generateOtpCode(userId);

    ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(smsPort, times(1)).send(phoneCaptor.capture(), messageCaptor.capture());
    assertEquals(newPhone, phoneCaptor.getValue());

    assertTrue(messageCaptor.getValue().contains(otpCode.toString()));
    assertTrue(
        messageCaptor.getValue().contains("Seu código de verificação para a Arena Máfia é:"));
  }

  @Test
  @DisplayName("Não deve enviar SMS se a geração de OTP falhar")
  void onUserRegistration_shouldNotSendSms_whenOtpGenerationFails() {
    // Arrange
    User user = createUser();
    OnVerificationRequiredEvent event = new OnVerificationRequiredEvent(user);

    when(otpPort.generateOtpCode(user.getId()))
        .thenThrow(new RuntimeException("Falha ao conectar com o Redis"));

    // Act & Assert
    assertDoesNotThrow(() -> eventListener.onUserRegistration(event));

    verify(smsPort, never()).send(anyString(), anyString());
  }
}
