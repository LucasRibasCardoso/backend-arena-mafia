package com.projetoExtensao.arenaMafia.infrastructure.web.user;

import com.projetoExtensao.arenaMafia.application.user.usecase.disable.DisableMyAccountUseCase;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

  private final InitiateChangePhoneUseCase initiateChangePhoneUseCase;
  private final CompleteChangePhoneUseCase completeChangePhoneUseCase;
  private final DisableMyAccountUseCase disableMyAccountUseCase;
  private final ChangeUsernameUseCase changeUsernameUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;
  private final UpdateProfileUseCase updateProfileUseCase;

  public UserController(
      CompleteChangePhoneUseCase completeChangePhoneUseCase,
      InitiateChangePhoneUseCase initiateChangePhoneUseCase,
      DisableMyAccountUseCase disableMyAccountUseCase,
      ChangeUsernameUseCase changeUsernameUseCase,
      ChangePasswordUseCase changePasswordUseCase,
      UpdateProfileUseCase updateProfileUseCase) {
    this.completeChangePhoneUseCase = completeChangePhoneUseCase;
    this.initiateChangePhoneUseCase = initiateChangePhoneUseCase;
    this.disableMyAccountUseCase = disableMyAccountUseCase;
    this.changeUsernameUseCase = changeUsernameUseCase;
    this.changePasswordUseCase = changePasswordUseCase;
    this.updateProfileUseCase = updateProfileUseCase;
  }

  @PatchMapping("/profile")
  public ResponseEntity<UserProfileResponseDTO> updateProfile(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody UpdateProfileRequestDTO requestDTO) {

    User updatedUser =
        updateProfileUseCase.execute(authenticatedUser.getUser().getId(), requestDTO);
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
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody ChangeUsernameRequestDTO request) {

    User updatedUser = changeUsernameUseCase.execute(authenticatedUser.getUser().getId(), request);
    UserProfileResponseDTO response =
        new UserProfileResponseDTO(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody ChangePasswordRequestDTO request) {

    changePasswordUseCase.execute(authenticatedUser.getUser().getId(), request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/phone/verification")
  public ResponseEntity<Void> initiatePhoneVerification(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody InitiateChangePhoneRequestDTO request) {

    initiateChangePhoneUseCase.execute(authenticatedUser.getUser().getId(), request);
    return ResponseEntity.accepted().build();
  }

  @PatchMapping("/phone/verification/confirm")
  public ResponseEntity<UserProfileResponseDTO> completePhoneVerification(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody CompletePhoneChangeRequestDTO request) {

    User updatedUser =
        completeChangePhoneUseCase.execute(authenticatedUser.getUser().getId(), request);
    UserProfileResponseDTO response =
        new UserProfileResponseDTO(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/disable")
  public ResponseEntity<Void> deactivateAccount(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {
    disableMyAccountUseCase.execute(authenticatedUser.getUser().getId());
    return ResponseEntity.noContent().build();
  }
}
