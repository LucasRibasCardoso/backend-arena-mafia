package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.usecase.LoginUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.SignUpUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.RefreshTokenRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
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
  private final SignUpUseCase signUpUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  public AuthController(
      LoginUseCase loginUseCase,
      SignUpUseCase signUpUseCase,
      RefreshTokenUseCase refreshTokenUseCase) {
    this.loginUseCase = loginUseCase;
    this.signUpUseCase = signUpUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
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
}
