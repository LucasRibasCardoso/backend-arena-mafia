package com.projetoExtensao.arenaMafia.application.auth.usecase.imp;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.SignUpUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignUpUseCaseImp implements SignUpUseCase {

  private final UserRepositoryPort userRepository;
  private final PhoneValidatorPort phoneValidator;
  private final PasswordEncoderPort passwordEncoderPort;
  private final ApplicationEventPublisher eventPublisher;

  public SignUpUseCaseImp(
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      PasswordEncoderPort passwordEncoderPort,
      ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.phoneValidator = phoneValidator;
    this.passwordEncoderPort = passwordEncoderPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public String execute(SignupRequestDto requestDto) {
    String username = requestDto.username();
    String fullName = requestDto.fullName();
    String phone = requestDto.phone();
    String password = requestDto.password();

    // Valida se o username e telefone são únicos
    validateUniqueness(username, phone);

    String formattedPhone = phoneValidator.formatToE164(phone);
    String encodedPassword = passwordEncoderPort.encode(password);

    User user = User.create(username, fullName, formattedPhone, encodedPassword);

    // Tenta salvar o usuário e captura exceções de integridade se o telefone ou username já existam
    User savedUser = userRepository.save(user);
    eventPublisher.publishEvent(new OnVerificationRequiredEvent(savedUser));
    return savedUser.getUsername();
  }

  private void validateUniqueness(String username, String phone) {
    if (userRepository.existsByUsername(username)) {
      throw new UserAlreadyExistsException("Esse nome de usuário já está em uso.");
    }
    if (userRepository.existsByPhone(phone)) {
      throw new UserAlreadyExistsException("Esse número de telefone já está em uso.");
    }
  }
}
