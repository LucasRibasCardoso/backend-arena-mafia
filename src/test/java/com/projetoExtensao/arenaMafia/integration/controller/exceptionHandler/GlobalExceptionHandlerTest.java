package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Testes de integração para GlobalExceptionHandler")
public class GlobalExceptionHandlerTest {

  private static final String BASE_URL = "/test/exceptions/global";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Deve capturar Exception e retornar status 500")
  void shouldThrowGenericException() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/generic"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(
            jsonPath("$.message")
                .value("Erro interno do servidor. Por favor, tente novamente mais tarde."))
        .andExpect(jsonPath("$.path").value(BASE_URL + "/generic"));
  }

  @Test
  @DisplayName("Deve capturar DataIntegrityViolationException e retornar status 409")
  void shouldThrowDataIntegrityException() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/data-integrity"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(
            jsonPath("$.message")
                .value("Erro de integridade de dados. Verifique os dados informados."))
        .andExpect(jsonPath("$.path").value(BASE_URL + "/data-integrity"));
  }

  @Test
  @DisplayName("Deve capturar MethodArgumentNotValidException e retornar status 400")
  void shouldThrowMethodArgumentNotValidExceptionWhenUsernameIsEmpty() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL + "/validation")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"fieldTestUsername\":\"usr\"}")) // username muito curto
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Erro de validação. Verifique os campos informados."))
        .andExpect(jsonPath("$.path").value(BASE_URL + "/validation"))
        .andExpect(jsonPath("$.fieldErrors[0].fieldName").value("fieldTestUsername"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message")
                .value("O campo usuário deve ter entre 4 e 50 caracteres"));
  }

  @Test
  @DisplayName("Deve capturar DomainValidationException e retornar status 400")
  void shouldThrowDomainValidationException() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/domain"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Erro de domínio simulado."))
        .andExpect(jsonPath("$.path").value(BASE_URL + "/domain"));
  }
}
