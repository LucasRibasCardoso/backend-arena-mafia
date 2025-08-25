package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("Testes de integração para AuthExceptionHandler")
public class AuthExceptionHandlerTest {

  private static final String BASE_URL = "/test/exceptions/auth";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Deve capturar AccessDeniedException e retornar status 403")
  void shouldThrowAccessDeniedException() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/access-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(
            jsonPath("$.message")
                .value("Acesso negado. Você não tem permissão para acessar este recurso."))
        .andExpect(jsonPath("$.path").value(BASE_URL + "/access-denied"));
  }

  @Nested
  @DisplayName("Deve capturar BadRefreshTokenException e retornar status 401")
  class BadRefreshTokenExceptionTests {
    @Test
    @DisplayName("Quando o refresh token estiver expirado")
    void shouldThrowBadRefreshTokenExceptionWhenTokenIsExpired() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/refresh-token-expired"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(
              jsonPath("$.message")
                  .value(
                      "Token de atualização inválido ou expirado. Por favor, faça login novamente."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/refresh-token-expired"));
    }

    @Test
    @DisplayName("Quando o refresh token não for encontrado")
    void shouldThrowNotFoundRefreshTokenExceptionWhenTokenIsNotFound() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/refresh-not-found"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(
              jsonPath("$.message")
                  .value(
                      "Token de atualização inválido ou expirado. Por favor, faça login novamente."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/refresh-not-found"));
    }

    @Test
    @DisplayName("Quando o refresh token for um formato inválido")
    void shouldThrowBadRefreshTokenExceptionWhenTokenIsInvalidFormat() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/invalid-format-refresh-token"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(
              jsonPath("$.message")
                  .value(
                      "Token de atualização inválido ou expirado. Por favor, faça login novamente."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/invalid-format-refresh-token"));
    }
  }

  @Nested
  @DisplayName("Deve capturar AccountException e retornar status 403")
  class AccountExceptionTests {

    @Test
    @DisplayName("Quando a conta estiver desativada")
    void shouldThrowAccountDisabledExceptionWhenAccountIsDisabled() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/disabled-account"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(
              jsonPath("$.message")
                  .value("Está conta não está ativa."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/disabled-account"));
    }

    @Test
    @DisplayName("Quando a conta estiver bloqueada")
    void shouldThrowAccountLockedExceptionWhenAccountIsLocked() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/account-locked"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(
              jsonPath("$.message")
                  .value("Sua conta está bloqueada. Por favor, contate o suporte."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/account-locked"));
    }

    @Test
    @DisplayName("Quando a conta não estiver verificada")
    void shouldThrowAccountNotVerifiedExceptionWhenAccountIsNotVerified() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/account-not-verified"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(
              jsonPath("$.message")
                  .value("Sua conta ainda não foi verificada."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/account-not-verified"));
    }

    @Test
    @DisplayName("Quando a conta não estiver ativada")
    void shouldThrowDisabledAccountExceptionWhenAccountIsNotActivated() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/disabled-account"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(
              jsonPath("$.message")
                  .value("Está conta não está ativa."))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/disabled-account"));
    }

    @Test
    @DisplayName("Deve capturar BadPhoneNumberException e retornar status 400")
    void shouldThrowBadPhoneNumberExceptionWhenPhoneNumberIsInvalid() throws Exception {
      mockMvc
          .perform(get(BASE_URL + "/bad-phone-number"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(
              jsonPath("$.message")
                  .value("Número de telefone inválido"))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/bad-phone-number"));
    }
  }
}
