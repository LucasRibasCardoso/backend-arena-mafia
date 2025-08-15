package com.projetoExtensao.arenaMafia.infrastructure.web.auth;

import com.projetoExtensao.arenaMafia.application.useCase.LoginUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class AuthController implements AuthControllerDocs {

  private final LoginUseCase loginUseCase;

  public AuthController(LoginUseCase loginUseCase) {
    this.loginUseCase = loginUseCase;
  }

  @PostMapping("/login")
  @Override
  public ResponseEntity<TokenResponseDto> login(
      @RequestBody @Valid LoginRequestDto loginRequestDto) {
    TokenResponseDto tokenResponse = loginUseCase.login(loginRequestDto);
    return ResponseEntity.ok(tokenResponse);
  }
}
