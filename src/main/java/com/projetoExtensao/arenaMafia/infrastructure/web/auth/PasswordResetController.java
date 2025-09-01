package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ForgotPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.GeneratePasswordResetTokenUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.ResetPasswordUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.util.CookieUtils;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

  @Value("${spring.security.jwt.refresh-token-expiration-days}")
  private long refreshTokenExpirationDays;

  private final CookieUtils cookieUtils;
  private final ResetPasswordUseCase resetPasswordUseCase;
  private final ForgotPasswordUseCase forgotPasswordUseCase;
  private final GeneratePasswordResetTokenUseCase GeneratePasswordResetTokenUseCase;

  public PasswordResetController(
      CookieUtils cookieUtils,
      ResetPasswordUseCase resetPasswordUseCase,
      ForgotPasswordUseCase forgotPasswordUseCase,
      GeneratePasswordResetTokenUseCase GeneratePasswordResetTokenUseCase) {
    this.cookieUtils = cookieUtils;
    this.resetPasswordUseCase = resetPasswordUseCase;
    this.forgotPasswordUseCase = forgotPasswordUseCase;
    this.GeneratePasswordResetTokenUseCase = GeneratePasswordResetTokenUseCase;
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<SimpleMessageResponseDto> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequestDto requestDto) {

    forgotPasswordUseCase.execute(requestDto);
    SimpleMessageResponseDto responseDto =
        new SimpleMessageResponseDto(
            "Se o telefone informado for válido, enviaremos um código para verificação da conta.");
    return ResponseEntity.ok(responseDto);
  }

  @PostMapping("/password-reset-token")
  public ResponseEntity<PasswordResetTokenResponseDto> forgotPasswordVerify(
      @Valid @RequestBody ValidateOtpRequestDto requestDto) {

    String passwordResetToken = GeneratePasswordResetTokenUseCase.execute(requestDto);
    PasswordResetTokenResponseDto responseDto =
        new PasswordResetTokenResponseDto(passwordResetToken);
    return ResponseEntity.ok(responseDto);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<SimpleMessageResponseDto> resetPassword(
      @Valid @RequestBody ResetPasswordRequestDto requestDto) {

    resetPasswordUseCase.execute(requestDto);
    ResponseCookie expiredCookie = cookieUtils.createRefreshTokenExpiredCookie();

    SimpleMessageResponseDto responseDto =
        new SimpleMessageResponseDto(
            "Senha redefinida com sucesso. Você já pode fazer o login com sua nova senha.");

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .body(responseDto);
  }
}
