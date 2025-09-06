package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LoginUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LogoutUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.SignUpUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.*;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.SignupResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.util.CookieUtils;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final CookieUtils cookieUtils;
  private final LoginUseCase loginUseCase;
  private final LogoutUseCase logoutUseCase;
  private final SignUpUseCase signUpUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  public AuthController(
      CookieUtils cookieUtils,
      LoginUseCase loginUseCase,
      LogoutUseCase logoutUseCase,
      SignUpUseCase signUpUseCase,
      RefreshTokenUseCase refreshTokenUseCase) {
    this.cookieUtils = cookieUtils;
    this.loginUseCase = loginUseCase;
    this.logoutUseCase = logoutUseCase;
    this.signUpUseCase = signUpUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
  }

  @PostMapping("/signup")
  public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto requestDto) {
    User savedUser = signUpUseCase.execute(requestDto);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .replacePath("/api/users/{identifier}")
            .buildAndExpand(savedUser.getId())
            .toUri();

    SignupResponseDto signupResponseDto =
        new SignupResponseDto(
            savedUser.getId().toString(),
            savedUser.getUsername(),
            AccountStatus.PENDING_VERIFICATION.getValue(),
            "Conta criada com sucesso. Um código de verificação foi enviado para o seu telefone.");

    return ResponseEntity.created(location).body(signupResponseDto);
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto) {
    AuthResult authResult = loginUseCase.execute(requestDto);

    ResponseCookie refreshTokenCookie =
        cookieUtils.createRefreshTokenCookie(authResult.refreshToken());

    User user = authResult.user();
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(
            user.getId().toString(),
            user.getPhone(),
            user.getUsername(),
            user.getFullName(),
            user.getRole().name(),
            authResult.accessToken());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(tokenResponseDto);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(value = "refreshToken", required = false) String requestDto) {

    logoutUseCase.execute(requestDto);
    ResponseCookie expiredCookie = cookieUtils.createRefreshTokenExpiredCookie();

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .build();
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<TokenResponseDto> refreshToken(
      @CookieValue(name = "refreshToken", required = false) String oldRefreshToken) {

    // Cria o DTO de solicitação contendo o refresh token antigo
    RefreshTokenRequestDto refreshTokenRequest = new RefreshTokenRequestDto(oldRefreshToken);

    // Cria o novo conjunto de tokens
    AuthResult authResult = refreshTokenUseCase.execute(refreshTokenRequest);

    // Criamos o cookie contendo o novo refresh token
    ResponseCookie refreshTokenCookie =
        cookieUtils.createRefreshTokenCookie(authResult.refreshToken());

    // Crie o DTO de resposta contendo o accessToken e o refreshToken
    User user = authResult.user();
    TokenResponseDto tokenResponseDto =
        new TokenResponseDto(
            user.getId().toString(),
            user.getPhone(),
            user.getUsername(),
            user.getFullName(),
            user.getRole().name(),
            authResult.accessToken());

    // Retorna o cookie contendo o novo refresh token e o accessToken via DTO
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(tokenResponseDto);
  }
}
