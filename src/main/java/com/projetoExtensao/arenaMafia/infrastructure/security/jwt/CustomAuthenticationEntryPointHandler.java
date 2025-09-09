package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import jakarta.servlet.RequestDispatcher;
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

    String originalPath = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    if (originalPath == null) {
      originalPath = request.getRequestURI();
    }

    ErrorCode errorCode = ErrorCode.SESSION_EXPIRED;
    var dto = ErrorResponseDto.forGeneralError(UNAUTHORIZED_STATUS, errorCode, originalPath);

    response.setStatus(UNAUTHORIZED_STATUS);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(objectMapper.writeValueAsString(dto));
  }
}
