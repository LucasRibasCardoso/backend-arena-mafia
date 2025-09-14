package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Testes de Integração para o GlobalExceptionHandler Unificado")
public class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;
  private static final String BASE_URL = "/test/exceptions";

  @Nested
  @DisplayName("Testes para Erros 400 Bad Request")
  class BadRequestTests {

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o campo otpSessionId for nulo")
    void shouldReturn400_whenOtpSessionIdIsNull() throws Exception {
      // Arrange
      ErrorCode expectedFieldErrorCode = ErrorCode.OTP_SESSION_ID_REQUIRED;
      String requestBody = "{\"otpCode\": \"123456\", \"otpSessionId\": null}";

      // Act & Assert
      mockMvc
          .perform(
              post(BASE_URL + "/bad-request/otp-session-invalid")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestBody))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errorCode", is(ErrorCode.VALIDATION_FAILED.name())))
          .andExpect(jsonPath("$.fieldErrors[0].fieldName", is("otpSessionId")))
          .andExpect(jsonPath("$.fieldErrors[0].errorCode", is(expectedFieldErrorCode.name())))
          .andExpect(
              jsonPath(
                  "$.fieldErrors[0].developerMessage", is(expectedFieldErrorCode.getMessage())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "not-a-valid-uuid"})
    @DisplayName("Deve retornar 400 Bad Request quando o otpSessionId tiver formato inválido")
    void shouldReturn400_whenOtpSessionIdIsMalformed(String invalidSessionId) throws Exception {
      // Arrange
      String requestBody =
          "{\"otpCode\": \"123456\", \"otpSessionId\": \"" + invalidSessionId + "\"}";

      // Determina o errorCode esperado com base na entrada
      ErrorCode expectedFieldErrorCode =
          invalidSessionId.trim().isEmpty()
              ? ErrorCode.OTP_SESSION_ID_REQUIRED
              : ErrorCode.OTP_SESSION_ID_INVALID_FORMAT;

      // Act & Assert
      mockMvc
          .perform(
              post(BASE_URL + "/bad-request/otp-session-invalid")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestBody))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errorCode", is(ErrorCode.VALIDATION_FAILED.name())))
          .andExpect(jsonPath("$.fieldErrors[0].fieldName", is("otpSessionId")))
          .andExpect(jsonPath("$.fieldErrors[0].errorCode", is(expectedFieldErrorCode.name())))
          .andExpect(
              jsonPath(
                  "$.fieldErrors[0].developerMessage", is(expectedFieldErrorCode.getMessage())));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 401 Unauthorized")
  class UnauthorizedTests {

    @Test
    @DisplayName("Deve capturar RefreshTokenExpiredException")
    void shouldHandleRefreshTokenExpiredException() throws Exception {
      ErrorCode expectedError = ErrorCode.REFRESH_TOKEN_INCORRECT_OR_EXPIRED;
      String expectedPath = BASE_URL + "/unauthorized/refresh-token-expired";

      mockMvc
          .perform(get(expectedPath))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status", is(401)))
          .andExpect(jsonPath("$.path", is(expectedPath)))
          .andExpect(jsonPath("$.errorCode", is(expectedError.name())))
          .andExpect(jsonPath("$.developerMessage", is(expectedError.getMessage())));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 403 Forbidden")
  class ForbiddenTests {
    @Test
    @DisplayName("Deve capturar AccessDeniedException")
    void shouldHandleAccessDeniedException() throws Exception {
      ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
      String expectedPath = BASE_URL + "/forbidden/access-denied";

      mockMvc
          .perform(get(expectedPath))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.path").value(expectedPath))
          .andExpect(jsonPath("$.errorCode").value(errorCode.name()))
          .andExpect(jsonPath("$.developerMessage").value(errorCode.getMessage()));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 404 Not Found")
  class NotFoundTests {
    @Test
    @DisplayName("Deve capturar NotFoundException (ex: UserNotFoundException)")
    void shouldHandleNotFoundException() throws Exception {
      ErrorCode expectedError = ErrorCode.USER_NOT_FOUND;
      String expectedPath = BASE_URL + "/not-found/user-not-found";

      mockMvc
          .perform(get(expectedPath))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status", is(404)))
          .andExpect(jsonPath("$.path", is(expectedPath)))
          .andExpect(jsonPath("$.errorCode", is(expectedError.name())))
          .andExpect(jsonPath("$.developerMessage", is(expectedError.getMessage())));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 409 Conflict")
  class ConflictTests {
    @Test
    @DisplayName("Deve capturar ConflictException (ex: UserAlreadyExistsException)")
    void shouldHandleConflictException() throws Exception {
      ErrorCode expectedError = ErrorCode.USERNAME_ALREADY_EXISTS;
      String expectedPath = BASE_URL + "/conflict/user-already-exists";

      mockMvc
          .perform(get(expectedPath))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status", is(409)))
          .andExpect(jsonPath("$.path", is(expectedPath)))
          .andExpect(jsonPath("$.errorCode", is(expectedError.name())))
          .andExpect(jsonPath("$.developerMessage", is(expectedError.getMessage())));
    }

    @Test
    @DisplayName("Deve capturar DataIntegrityViolationException")
    void shouldHandleDataIntegrityViolationException() throws Exception {
      ErrorCode expectedError = ErrorCode.DATA_INTEGRITY_VIOLATION;
      String expectedPath = BASE_URL + "/conflict/data-integrity";

      mockMvc
          .perform(get(expectedPath))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status", is(409)))
          .andExpect(jsonPath("$.path", is(expectedPath)))
          .andExpect(jsonPath("$.errorCode", is(expectedError.name())))
          .andExpect(jsonPath("$.developerMessage", is(expectedError.getMessage())));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 500 Internal Server Error")
  class InternalServerErrorTests {
    @Test
    @DisplayName("Deve capturar Exception genérica")
    void shouldHandleGenericException() throws Exception {
      ErrorCode expectedError = ErrorCode.UNEXPECTED_ERROR;
      String expectedPath = BASE_URL + "/internal-server-error";

      mockMvc
          .perform(get(expectedPath))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.status", is(500)))
          .andExpect(jsonPath("$.path", is(expectedPath)))
          .andExpect(jsonPath("$.errorCode", is(expectedError.name())))
          .andExpect(jsonPath("$.developerMessage", is(expectedError.getMessage())));
    }
  }
}
