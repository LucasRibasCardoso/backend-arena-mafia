package com.projetoExtensao.arenaMafia.infrastructure.web.user;

import com.projetoExtensao.arenaMafia.application.user.usecase.disable.DisableMyAccountUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.password.ChangePasswordUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.CompleteChangePhoneUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.InitiateChangePhoneUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.ResendChangePhoneOtpUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.GetUserProfileUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.UpdateProfileUseCase;
import com.projetoExtensao.arenaMafia.application.user.usecase.username.ChangeUsernameUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.response.UserProfileResponseDto;
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
  private final ResendChangePhoneOtpUseCase resendChangePhoneOtpUseCase;
  private final ChangeUsernameUseCase changeUsernameUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;
  private final GetUserProfileUseCase getUserProfileUseCase;
  private final UpdateProfileUseCase updateProfileUseCase;

  public UserController(
      CompleteChangePhoneUseCase completeChangePhoneUseCase,
      InitiateChangePhoneUseCase initiateChangePhoneUseCase,
      DisableMyAccountUseCase disableMyAccountUseCase,
      ResendChangePhoneOtpUseCase resendChangePhoneOtpUseCase,
      ChangeUsernameUseCase changeUsernameUseCase,
      ChangePasswordUseCase changePasswordUseCase,
      GetUserProfileUseCase getUserProfileUseCase,
      UpdateProfileUseCase updateProfileUseCase) {
    this.completeChangePhoneUseCase = completeChangePhoneUseCase;
    this.initiateChangePhoneUseCase = initiateChangePhoneUseCase;
    this.disableMyAccountUseCase = disableMyAccountUseCase;
    this.changeUsernameUseCase = changeUsernameUseCase;
    this.changePasswordUseCase = changePasswordUseCase;
    this.getUserProfileUseCase = getUserProfileUseCase;
    this.updateProfileUseCase = updateProfileUseCase;
    this.resendChangePhoneOtpUseCase = resendChangePhoneOtpUseCase;
  }

  @GetMapping
  public ResponseEntity<UserProfileResponseDto> getMyProfile(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {

    User user = getUserProfileUseCase.execute(authenticatedUser.getUser().getId());
    UserProfileResponseDto response =
        new UserProfileResponseDto(
            user.getUsername(), user.getFullName(), user.getPhone(), user.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/profile")
  public ResponseEntity<UserProfileResponseDto> updateProfile(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody UpdateProfileRequestDto requestDTO) {

    User updatedUser =
        updateProfileUseCase.execute(authenticatedUser.getUser().getId(), requestDTO);
    UserProfileResponseDto response =
        new UserProfileResponseDto(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/username")
  public ResponseEntity<UserProfileResponseDto> changeUsername(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody ChangeUsernameRequestDto request) {

    User updatedUser = changeUsernameUseCase.execute(authenticatedUser.getUser().getId(), request);
    UserProfileResponseDto response =
        new UserProfileResponseDto(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody ChangePasswordRequestDto request) {

    changePasswordUseCase.execute(authenticatedUser.getUser().getId(), request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/phone/verification")
  public ResponseEntity<Void> initiatePhoneVerification(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody InitiateChangePhoneRequestDto request) {

    initiateChangePhoneUseCase.execute(authenticatedUser.getUser().getId(), request);
    return ResponseEntity.accepted().build();
  }

  @PatchMapping("/phone/verification/confirm")
  public ResponseEntity<UserProfileResponseDto> completePhoneVerification(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser,
      @Valid @RequestBody CompletePhoneChangeRequestDto request) {

    User updatedUser =
        completeChangePhoneUseCase.execute(authenticatedUser.getUser().getId(), request);
    UserProfileResponseDto response =
        new UserProfileResponseDto(
            updatedUser.getUsername(),
            updatedUser.getFullName(),
            updatedUser.getPhone(),
            updatedUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/phone/verification/resend")
  public ResponseEntity<Void> resendPhoneVerificationCode(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {

    resendChangePhoneOtpUseCase.execute(authenticatedUser.getUser().getId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/disable")
  public ResponseEntity<Void> deactivateAccount(
      @AuthenticationPrincipal UserDetailsAdapter authenticatedUser) {
    disableMyAccountUseCase.execute(authenticatedUser.getUser().getId());
    return ResponseEntity.noContent().build();
  }
}
