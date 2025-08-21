package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPointHandler implements AuthenticationEntryPoint {

  private static final int UNAUTHORIZED_STATUS = HttpServletResponse.SC_UNAUTHORIZED;

  private final ObjectMapper objectMapper;

  public CustomAuthenticationEntryPointHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.setStatus(UNAUTHORIZED_STATUS);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    objectMapper.writeValue(response.getOutputStream(), createErrorResponse(request));
  }

  private ErrorResponseDto createErrorResponse(HttpServletRequest request) {
    String errorMessage = "Ocorreu um erro de autenticação: acesso não autorizado.";

    return ErrorResponseDto.forGeneralError(
        UNAUTHORIZED_STATUS, errorMessage, request.getRequestURI());
  }
}
