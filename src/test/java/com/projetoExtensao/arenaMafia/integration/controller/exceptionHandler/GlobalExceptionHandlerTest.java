package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Testes de Integração para o GlobalExceptionHandler Unificado")
public class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;
  private static final String BASE_URL = "/test/exceptions";

  @Nested
  @DisplayName("Testes para Erros 400 Bad Request")
  class BadRequestTests {
    @Test
    @DisplayName("Deve capturar BadRequestException (ex: BadPhoneNumberException)")
    void shouldHandleBadRequestException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/bad-request/bad-phone-number"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.message").value("Número de telefone inválido."));
    }

    @Test
    @DisplayName("Deve capturar MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValidException() throws Exception {
      mockMvc.perform(post(BASE_URL + "/method-argument-not-valid")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"fieldTestUsername\":\"a\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.message").value("Erro de validação. Verifique os campos informados."))
          .andExpect(jsonPath("$.fieldErrors[0].fieldName").value("fieldTestUsername"));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 403 Forbidden")
  class ForbiddenTests {
    @Test
    @DisplayName("Deve capturar ForbiddenException (ex: AccountLockedException)")
    void shouldHandleForbiddenException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/forbidden/account-locked"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.message").value("Sua conta está bloqueada."));
    }

    @Test
    @DisplayName("Deve capturar AccessDeniedException")
    void shouldHandleAccessDeniedException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/forbidden/access-denied"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.message").value("Acesso negado. Você não tem permissão para acessar este recurso."));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 404 Not Found")
  class NotFoundTests {
    @Test
    @DisplayName("Deve capturar NotFoundException (ex: UserNotFoundException)")
    void shouldHandleNotFoundException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/not-found/user-not-found"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 409 Conflict")
  class ConflictTests {
    @Test
    @DisplayName("Deve capturar ConflictException (ex: UserAlreadyExistsException)")
    void shouldHandleConflictException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/conflict/user-already-exists"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.message").value("Usuário já existe."));
    }

    @Test
    @DisplayName("Deve capturar DataIntegrityViolationException")
    void shouldHandleDataIntegrityViolationException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/conflict/data-integrity"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.message").value("Conflito de dados. O recurso que você está tentando criar ou atualizar já existe."));
    }
  }

  @Nested
  @DisplayName("Testes para Erros 500 Internal Server Error")
  class InternalServerErrorTests {
    @Test
    @DisplayName("Deve capturar Exception genérica")
    void shouldHandleGenericException() throws Exception {
      mockMvc.perform(get(BASE_URL + "/internal-server-error"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.message").value("Erro interno do servidor. Por favor, tente novamente mais tarde."));
    }
  }
}
