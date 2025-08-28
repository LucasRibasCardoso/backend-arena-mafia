package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.imp.ForgotPasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ForgotPasswordUseCase")
public class ForgotPasswordUseCaseImpTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ForgotPasswordUseCaseImp forgotPasswordUseCase;

  @Test
  @DisplayName(
      "Deve publicar um evento para envio de sms quando o usuário for encontrado pelo telefone")
  void execute_shouldPublishEvent_whenUserIsFoundByPhone() {
    // Arrange
    var requestDto = new ForgotPasswordRequestDto("+5547988887777");
    String formattedPhone = "+5547988887777";
    User mockUser = mock(User.class);

    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(mockUser));

    // Act
    forgotPasswordUseCase.execute(requestDto);

    // Assert
    ArgumentCaptor<OnVerificationRequiredEvent> eventCaptor =
        ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    OnVerificationRequiredEvent publishedEvent = eventCaptor.getValue();

    assertThat(publishedEvent.user()).isEqualTo(mockUser);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
  }

  @Test
  @DisplayName("NÃO deve publicar um evento quando o usuário não for encontrado (Segurança)")
  void execute_shouldDoNothing_whenUserIsNotFound() {
    // Arrange
    var requestDto = new ForgotPasswordRequestDto("+5547911112222");
    String formattedPhone = "+5547911112222";

    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act
    forgotPasswordUseCase.execute(requestDto);

    // Assert
    verify(eventPublisher, never()).publishEvent(any());
    verify(userRepository, times(1)).findByPhone(formattedPhone);
  }

  @Test
  @DisplayName("Deve propagar BadPhoneNumberException para um formato de telefone inválido")
  void execute_shouldPropagateException_whenPhoneFormatIsInvalid() {
    // Arrange
    var requestDto = new ForgotPasswordRequestDto("invalid-phone");
    when(phoneValidator.formatToE164("invalid-phone"))
        .thenThrow(new BadPhoneNumberException("Número de telefone inválido."));

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage("Número de telefone inválido.");

    // Verify
    verify(userRepository, never()).findByPhone(anyString());
    verify(eventPublisher, never()).publishEvent(any());
  }
}
