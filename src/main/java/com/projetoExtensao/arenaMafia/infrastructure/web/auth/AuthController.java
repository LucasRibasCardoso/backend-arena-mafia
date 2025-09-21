package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.VerifyAccountUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LoginUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LogoutUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.SignUpUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.otp.ResendOtpUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ForgotPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ResetPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ValidatePasswordResetOtpUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.AuthResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.util.CookieUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final CookieUtils cookieUtils;
  private final LoginUseCase loginUseCase;
  private final LogoutUseCase logoutUseCase;
  private final SignUpUseCase signUpUseCase;
  private final ResendOtpUseCase resendOtpUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;
  private final VerifyAccountUseCase verifyAccountUseCase;
  private final ResetPasswordUseCase resetPasswordUseCase;
  private final ForgotPasswordUseCase forgotPasswordUseCase;
  private final ValidatePasswordResetOtpUseCase ValidatePasswordResetOtpUseCase;

  public AuthController(
      CookieUtils cookieUtils,
      LoginUseCase loginUseCase,
      LogoutUseCase logoutUseCase,
      SignUpUseCase signUpUseCase,
      ResendOtpUseCase resendOtpUseCase,
      RefreshTokenUseCase refreshTokenUseCase,
      VerifyAccountUseCase verifyAccountUseCase,
      ResetPasswordUseCase resetPasswordUseCase,
      ForgotPasswordUseCase forgotPasswordUseCase,
      ValidatePasswordResetOtpUseCase validatePasswordResetOtpUseCase) {
    this.cookieUtils = cookieUtils;
    this.loginUseCase = loginUseCase;
    this.logoutUseCase = logoutUseCase;
    this.signUpUseCase = signUpUseCase;
    this.resendOtpUseCase = resendOtpUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
    this.verifyAccountUseCase = verifyAccountUseCase;
    this.resetPasswordUseCase = resetPasswordUseCase;
    this.forgotPasswordUseCase = forgotPasswordUseCase;
    this.ValidatePasswordResetOtpUseCase = validatePasswordResetOtpUseCase;
  }

  @PostMapping("/signup")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
    OtpSessionId otpSessionId = signUpUseCase.execute(request);

    SignupResponseDto signupResponseDto =
        new SignupResponseDto(
            otpSessionId,
            "Conta criada com sucesso. Um código de verificação foi enviado para o seu telefone.");

    return ResponseEntity.accepted().body(signupResponseDto);
  }

  @PostMapping("/verify-account")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<AuthResponseDto> verifyAccount(
      @Valid @RequestBody ValidateOtpRequestDto request) {
    AuthResult authResult = verifyAccountUseCase.execute(request);
    return buildAuthResponse(authResult);
  }

  @PostMapping("/resend-otp")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequestDto request) {
    resendOtpUseCase.execute(request.otpSessionId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  @CustomRateLimiter(limiterName = "loginRateLimiter")
  public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
    AuthResult authResult = loginUseCase.execute(request);
    return buildAuthResponse(authResult);
  }

  @PostMapping("/logout")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<Void> logout(
      @CookieValue(value = "refreshToken", required = false) String refreshToken) {

    RefreshTokenVO refreshTokenVo = RefreshTokenVO.fromString(refreshToken);
    logoutUseCase.execute(refreshTokenVo);
    ResponseCookie expiredCookie = cookieUtils.createRefreshTokenExpiredCookie();
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .build();
  }

  @PostMapping("/refresh-token")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<AuthResponseDto> refreshToken(
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {

    RefreshTokenVO refreshTokenVo = RefreshTokenVO.fromString(refreshToken);
    AuthResult authResult = refreshTokenUseCase.execute(refreshTokenVo);
    return buildAuthResponse(authResult);
  }

  @PostMapping("/forgot-password")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequestDto request) {
    ForgotPasswordResponseDto response = forgotPasswordUseCase.execute(request);
    return ResponseEntity.accepted().body(response);
  }

  @PostMapping("/reset-password-token")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<PasswordResetTokenResponseDto> forgotPasswordVerify(
      @Valid @RequestBody ValidateOtpRequestDto request) {
    PasswordResetTokenResponseDto response = ValidatePasswordResetOtpUseCase.execute(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/reset-password")
  @CustomRateLimiter(limiterName = "sensitiveOperationLimiter")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
    resetPasswordUseCase.execute(request);
    ResponseCookie expiredCookie = cookieUtils.createRefreshTokenExpiredCookie();

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .build();
  }

  private ResponseEntity<AuthResponseDto> buildAuthResponse(AuthResult authResult) {
    ResponseCookie refreshTokenCookie =
        cookieUtils.createRefreshTokenCookie(authResult.refreshToken());

    User user = authResult.user();
    AuthResponseDto authResponseDto =
        new AuthResponseDto(
            user.getId().toString(),
            user.getPhone(),
            user.getUsername(),
            user.getFullName(),
            user.getRole().name(),
            authResult.accessToken());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(authResponseDto);
  }
}
