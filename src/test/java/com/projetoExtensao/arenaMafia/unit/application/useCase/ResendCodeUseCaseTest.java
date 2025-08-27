package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.ResendCodeUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
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
@DisplayName("Testes unitários para ResendCodeUseCase")
public class ResendCodeUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ResendCodeUseCaseImp resendCodeUseCase;

  @Test
  @DisplayName(
      "Deve publicar um evento de verificação quando o usuário for encontrado e estiver pendente")
  void execute_shouldPublishEvent_whenUserIsFoundAndPending() {
    // Arrange
    var requestDto = new ResendCodeRequestDto("testuser");
    User userPendingVerification =
        User.create("testuser", "Test User", "+5547988887777", "hashedPassword");

    when(userRepository.findByUsername("testuser"))
        .thenReturn(Optional.of(userPendingVerification));

    // Act
    resendCodeUseCase.execute(requestDto);

    // Assert
    ArgumentCaptor<OnVerificationRequiredEvent> eventCaptor =
        ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    OnVerificationRequiredEvent publishedEvent = eventCaptor.getValue();

    assertThat(publishedEvent.user()).isEqualTo(userPendingVerification);
    verify(userRepository, times(1)).findByUsername("testuser");
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    var requestDto = new ResendCodeRequestDto("nonexistent");
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(requestDto))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage(
            "Usuário não encontrado para reenviar o código. Por favor realize o cadastro novamente.");

    // Verify
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta não estiver pendente de verificação")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsNotPending() {
    // Arrange
    var requestDto = new ResendCodeRequestDto("activeuser");
    User activeUser =
        User.reconstitute(
            java.util.UUID.randomUUID(),
            "activeuser",
            "Active User",
            "+5547911112222",
            "hashedPassword",
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            java.time.Instant.now());

    when(userRepository.findByUsername("activeuser")).thenReturn(Optional.of(activeUser));

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Está conta não está pendente para verificação.");

    // Verify
    verify(eventPublisher, never()).publishEvent(any());
  }
}
