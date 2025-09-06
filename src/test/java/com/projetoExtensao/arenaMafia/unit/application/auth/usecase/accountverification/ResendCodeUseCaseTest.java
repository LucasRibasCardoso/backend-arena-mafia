package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.accountverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp.ResendCodeUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUserIdentifierException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

  private User createUser(AccountStatus status) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "hashedpassword",
        status,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName(
      "Deve disparar um evento de verificação quando o usuário for encontrado e estiver pendente")
  void execute_shouldPublishEvent_whenUserExists() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    UUID userId = user.getId();
    var request = new ResendCodeRequestDto(userId.toString());

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act
    resendCodeUseCase.execute(request);

    // Assert
    ArgumentCaptor<OnVerificationRequiredEvent> eventCaptor =
        ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    OnVerificationRequiredEvent publishedEvent = eventCaptor.getValue();

    assertThat(publishedEvent.user()).isEqualTo(user);
    verify(userRepository, times(1)).findById(userId);
  }

  @Test
  @DisplayName("Não deve disparar um evento de verificação quando um usuário não for encontrado")
  void execute_shouldDoNothing_whenUserIsNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    var request = new ResendCodeRequestDto(userId.toString());

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act
    assertDoesNotThrow(() -> resendCodeUseCase.execute(request));

    // Verify
    verify(userRepository, times(1)).findById(userId);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Deve lançar um exceção quando o userId for vazio ou nulo")
  void execute_shouldThrowException_whenUserIdIsNullOrEmpty(String userId) {
    // Arrange
    var request = new ResendCodeRequestDto(userId);

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(request))
        .isInstanceOf(InvalidUserIdentifierException.class)
        .hasMessage("Identificador de usuário não pode estar vazio.");

    verify(userRepository, never()).findByPhone(anyString());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o userId for inválido")
  void execute_shouldThrowException_whenUserIdIsInvalid() {
    // Arrange
    String userId = "invalid-user-id";
    var request = new ResendCodeRequestDto(userId);

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(request))
        .isInstanceOf(InvalidUserIdentifierException.class)
        .hasMessage("Identificador de usuário inválido.");

    verify(userRepository, never()).findByPhone(anyString());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta não estiver pendente de verificação")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsNotPending() {
    // Arrange
    User activeUser = createUser(AccountStatus.ACTIVE);
    UUID userId = activeUser.getId();
    var request = new ResendCodeRequestDto(activeUser.getId().toString());

    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Só é possível reenviar o código para contas pendentes de verificação.");

    // Verify
    verify(userRepository, times(1)).findById(userId);
    verify(eventPublisher, never()).publishEvent(any());
  }
}
