package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.ResendCodeUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.VerifyAccountUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import jakarta.validation.Valid;
import java.time.Duration;
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
public class AccountVerificationController {

  @Value("${spring.security.jwt.refresh-token-expiration-days}")
  private long refreshTokenExpirationDays;

  private final ResendCodeUseCase resendCodeUseCase;
  private final VerifyAccountUseCase verifyAccountUseCase;

  public AccountVerificationController(
      ResendCodeUseCase resendCodeUseCase, VerifyAccountUseCase verifyAccountUseCase) {
    this.resendCodeUseCase = resendCodeUseCase;
    this.verifyAccountUseCase = verifyAccountUseCase;
  }

  @PostMapping("/verify-account")
  public ResponseEntity<TokenResponseDto> verifyAccount(
      @Valid @RequestBody ValidateOtpRequestDto requestDto) {

    AuthResult authResult = verifyAccountUseCase.execute(requestDto);

    ResponseCookie refreshTokenCookie =
        ResponseCookie.from("refreshToken", authResult.refreshToken())
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .maxAge(Duration.ofDays(refreshTokenExpirationDays))
            .sameSite("Strict")
            .build();

    User user = authResult.user();
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(
            user.getPhone(),
            user.getUsername(),
            user.getFullName(),
            user.getRole().name(),
            authResult.accessToken());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(tokenResponseDto);
  }

  @PostMapping("/resend-code")
  public ResponseEntity<SimpleMessageResponseDto> resendVerificationCode(
      @Valid @RequestBody ResendCodeRequestDto requestDto) {

    resendCodeUseCase.execute(requestDto);

    SimpleMessageResponseDto responseDto =
        new SimpleMessageResponseDto("Código de verificação reenviado com sucesso.");

    return ResponseEntity.ok(responseDto);
  }
}
