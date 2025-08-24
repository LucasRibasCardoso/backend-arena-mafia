package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Testes de integração para UserExceptionHandler")
public class UserExceptionHandlerTest {

  private static final String BASE_URL = "/test/exceptions/user";

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Deve capturar BadPhoneNumberException e retornar status 400")
  void shouldThrowBadPhoneNumberExceptionWhenPhoneNumberIsInvalid() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/bad-phone-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(
            jsonPath("$.message")
                .value("Número de telefone inválido."))
        .andExpect(jsonPath("$.path").value(BASE_URL + "/bad-phone-number"));
  }
}
