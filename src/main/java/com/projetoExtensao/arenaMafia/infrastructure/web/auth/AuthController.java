package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.usecase.*;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.ForgotPasswordUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.GeneratePasswordResetTokenUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.ResetPasswordUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.SimpleMessageResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("api/auth")
public class AuthController {

  @Value("${spring.security.jwt.refresh-token-expiration-days}")
  private long refreshTokenExpirationDays;

  private final LoginUseCase loginUseCase;
  private final LogoutUseCase logoutUseCase;
  private final SignUpUseCase signUpUseCase;
  private final ResendCodeUseCase resendCodeUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;
  private final ResetPasswordUseCase resetPasswordUseCase;
  private final VerifyAccountUseCase verifyAccountUseCase;
  private final ForgotPasswordUseCase forgotPasswordUseCase;
  private final GeneratePasswordResetTokenUseCase GeneratePasswordResetTokenUseCase;

  public AuthController(
      LoginUseCase loginUseCase,
      LogoutUseCase logoutUseCase,
      SignUpUseCase signUpUseCase,
      ResendCodeUseCase resendCodeUseCase,
      RefreshTokenUseCase refreshTokenUseCase,
      ResetPasswordUseCase resetPasswordUseCase,
      VerifyAccountUseCase verifyAccountUseCase,
      ForgotPasswordUseCase forgotPasswordUseCase,
      GeneratePasswordResetTokenUseCase GeneratePasswordResetTokenUseCase) {
    this.loginUseCase = loginUseCase;
    this.logoutUseCase = logoutUseCase;
    this.signUpUseCase = signUpUseCase;
    this.resendCodeUseCase = resendCodeUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
    this.resetPasswordUseCase = resetPasswordUseCase;
    this.verifyAccountUseCase = verifyAccountUseCase;
    this.forgotPasswordUseCase = forgotPasswordUseCase;
    this.GeneratePasswordResetTokenUseCase = GeneratePasswordResetTokenUseCase;
  }

  @PostMapping("/signup")
  public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto requestDto) {
    String userIdentifier = signUpUseCase.execute(requestDto);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath("/api/users/{identifier}")
            .buildAndExpand(userIdentifier)
            .toUri();

    SignupResponseDto signupResponseDto =
        new SignupResponseDto(
            AccountStatus.PENDING_VERIFICATION.getValue(),
            "Conta criada com sucesso. Um código de verificação foi enviado para o seu telefone.",
            userIdentifier);

    return ResponseEntity.created(location).body(signupResponseDto);
  }

  @PostMapping("/verify-account")
  public ResponseEntity<TokenResponseDto> verifyAccount(
      @Valid @RequestBody ValidateOtpRequestDto requestDto) {

    // Verifica a conta do usuário e gera o conjunto de tokens
    AuthResult authResult = verifyAccountUseCase.execute(requestDto);

    // Cria o cookie contendo o refresh token
    ResponseCookie refreshTokenCookie = createRefreshTokenCookie(authResult.refreshToken());

    // Crie o DTO de resposta contendo o username e o accesToken
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(authResult.username(), authResult.accessToken());

    // Retorna o cookie contendo o refreshToken e o accessToken via DTO
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

  @PostMapping("/login")
  public ResponseEntity<TokenResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto) {

    // Realiza a autenticação do usuário e gera o conjunto de tokens
    AuthResult authResult = loginUseCase.execute(requestDto);

    // Cria o cookie contendo o refresh token
    ResponseCookie refreshTokenCookie = createRefreshTokenCookie(authResult.refreshToken());

    // Crie o DTO de resposta contendo o username e o accesToken
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(authResult.username(), authResult.accessToken());

    // Retorna o cookie contendo o refreshToken e o accessToken via DTO
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(tokenResponseDto);
  }

  @PostMapping("/logout")
  public ResponseEntity<SimpleMessageResponseDto> logout(
      @CookieValue(value = "refreshToken", required = false) String requestDto) {

    logoutUseCase.execute(requestDto);
    ResponseCookie expiredCookie = createRefreshTokenExpiredCookie();

    SimpleMessageResponseDto responseDto =
        new SimpleMessageResponseDto("Logout realizado com sucesso.");
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .body(responseDto);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<TokenResponseDto> refreshToken(
      @CookieValue(name = "refreshToken") String oldRefreshToken) {

    // Cria o DTO de solicitação contendo o refresh token antigo
    RefreshTokenRequestDto refreshTokenRequest = new RefreshTokenRequestDto(oldRefreshToken);

    // Cria o novo conjunto de tokens
    AuthResult authResult = refreshTokenUseCase.execute(refreshTokenRequest);

    // Criamos o cookie contendo o novo refresh token
    ResponseCookie refreshTokenCookie = createRefreshTokenCookie(authResult.refreshToken());

    // Crie o DTO de resposta contendo o accessToken e o refreshToken
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(authResult.username(), authResult.accessToken());

    // Retorna o cookie contendo o novo refresh token e o accessToken via DTO
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(tokenResponseDto);
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

  @PostMapping("/generate-password-reset-token")
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
    ResponseCookie expiredCookie = createRefreshTokenExpiredCookie();

    SimpleMessageResponseDto responseDto =
        new SimpleMessageResponseDto(
            "Senha redefinida com sucesso. Você já pode fazer o login com sua nova senha.");

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .body(responseDto);
  }

  private ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/api/auth")
        .maxAge(Duration.ofDays(refreshTokenExpirationDays))
        .sameSite("Strict")
        .build();
  }

  private ResponseCookie createRefreshTokenExpiredCookie() {
    return ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(true)
        .path("/api/auth")
        .maxAge(0) // Definido como 0 para expirar imediatamente o cookie
        .sameSite("Strict")
        .build();
  }
}
