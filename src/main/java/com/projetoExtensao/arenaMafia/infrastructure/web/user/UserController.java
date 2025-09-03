package com.projetoExtensao.arenaMafia.infrastructure.web.user;

import com.projetoExtensao.arenaMafia.application.user.usecase.password.ChangePasswordUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.CompleteChangePhoneUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.InitiateChangePhoneUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.UpdateProfileUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.username.ChangeUsernameUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.response.UserProfileResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

  private final InitiateChangePhoneUseCase initiateChangePhoneUseCase;
  private final CompleteChangePhoneUseCase completeChangePhoneUseCase;
  private final ChangeUsernameUseCase changeUsernameUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;
  private final UpdateProfileUseCase updateProfileUseCase;

  public UserController(
      CompleteChangePhoneUseCase completeChangePhoneUseCase,
      InitiateChangePhoneUseCase initiateChangePhoneUseCase,
      ChangeUsernameUseCase changeUsernameUseCase,
      ChangePasswordUseCase changePasswordUseCase,
      UpdateProfileUseCase updateProfileUseCase) {
    this.completeChangePhoneUseCase = completeChangePhoneUseCase;
    this.initiateChangePhoneUseCase = initiateChangePhoneUseCase;
    this.changeUsernameUseCase = changeUsernameUseCase;
    this.changePasswordUseCase = changePasswordUseCase;
    this.updateProfileUseCase = updateProfileUseCase;
  }

  @PatchMapping("/profile")
  public ResponseEntity<UserProfileResponseDTO> updateProfile(
      @Valid @RequestBody UpdateProfileRequestDTO requestDTO) {

    User authenticatedUser = getAuthenticatedUser();
    User updatedUser = updateProfileUseCase.execute(authenticatedUser.getId(), requestDTO);
    UserProfileResponseDTO response =
        new UserProfileResponseDTO(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/username")
  public ResponseEntity<UserProfileResponseDTO> changeUsername(
      @Valid @RequestBody ChangeUsernameDTO request) {

    User authenticatedUser = getAuthenticatedUser();
    User updatedUser = changeUsernameUseCase.execute(authenticatedUser.getId(), request);
    UserProfileResponseDTO response =
        new UserProfileResponseDTO(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {

    User authenticatedUser = getAuthenticatedUser();
    changePasswordUseCase.execute(authenticatedUser.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/phone/verification")
  public ResponseEntity<UserProfileResponseDTO> initiatePhoneVerification(
      @Valid @RequestBody ChangePhoneRequestDTO request) {

    User authenticatedUser = getAuthenticatedUser();
    initiateChangePhoneUseCase.execute(authenticatedUser.getId(), request);
    return ResponseEntity.accepted().build();
  }

  @PatchMapping("/phone/verification")
  public ResponseEntity<UserProfileResponseDTO> completePhoneVerification(
      @Valid @RequestBody CompletePhoneChangeRequestDTO request) {

    User authenticatedUser = getAuthenticatedUser();
    User updatedUser = completeChangePhoneUseCase.execute(authenticatedUser.getId(), request);
    UserProfileResponseDTO response =
        new UserProfileResponseDTO(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  private User getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserDetailsAdapter userDetails = (UserDetailsAdapter) authentication.getPrincipal();
    return userDetails.getUser();
  }
}
