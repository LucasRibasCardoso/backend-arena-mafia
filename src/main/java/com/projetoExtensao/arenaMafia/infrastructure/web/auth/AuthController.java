package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.usecase.*;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.MessageResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
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
  private final VerifyAccountUseCase verifyAccountUseCase;

  public AuthController(
      LoginUseCase loginUseCase,
      LogoutUseCase logoutUseCase,
      SignUpUseCase signUpUseCase,
      ResendCodeUseCase resendCodeUseCase,
      RefreshTokenUseCase refreshTokenUseCase,
      VerifyAccountUseCase verifyAccountUseCase) {
    this.loginUseCase = loginUseCase;
    this.logoutUseCase = logoutUseCase;
    this.signUpUseCase = signUpUseCase;
    this.resendCodeUseCase = resendCodeUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
    this.verifyAccountUseCase = verifyAccountUseCase;
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
  public ResponseEntity<MessageResponseDto> resendVerificationCode(
      @Valid @RequestBody ResendCodeRequestDto requestDto) {

    resendCodeUseCase.execute(requestDto);

    MessageResponseDto responseDto =
        new MessageResponseDto("Código de verificação reenviado com sucesso.");

    return ResponseEntity.ok(responseDto);
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponseDto> login(
      @RequestBody @Valid LoginRequestDto loginRequestDto) {

    // Realiza a autenticação do usuário e gera o conjunto de tokens
    AuthResult authResult = loginUseCase.execute(loginRequestDto);

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
  public ResponseEntity<MessageResponseDto> logout(
      @CookieValue(value = "refreshToken", required = false) String refreshToken) {

    logoutUseCase.execute(refreshToken);
    ResponseCookie expiredCookie = createRefreshTokenExpiredCookie();

    MessageResponseDto responseDto = new MessageResponseDto("Logout realizado com sucesso.");
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
