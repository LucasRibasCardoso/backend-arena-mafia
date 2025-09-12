package com.projetoExtensao.arenaMafia.domain.exception;

public enum ErrorCode {
  // Erros globais
  MALFORMED_JSON_REQUEST("Requisição JSON malformada."),
  UNEXPECTED_ERROR("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."),
  ACCESS_DENIED("Acesso negado. Você não tem permissão para acessar este recurso."),
  DATA_INTEGRITY_VIOLATION("O recurso que você está tentando criar ou atualizar já existe."),
  INVALID_REQUEST_PARAMETER("Um parâmetro da requisição é inválido ou está malformado."),
  VALIDATION_FAILED("A validação falhou. Verifique os detalhes dos campos para mais informações."),
  SESSION_EXPIRED("Sua sessão expirou. Por favor, faça login novamente."),

  JWT_TOKEN_INVALID_OR_EXPIRED("Token JWT expirado ou inválido."),
  INVALID_CREDENTIALS("Credenciais inválidas. Por favor, verifique seu usuário e senha."),

  ACCOUNT_STATE_CONFLICT("O status atual da conta não permite essa operação."),
  ACCOUNT_PENDING_VERIFICATION("Você precisa ativar sua conta. Conclua o processo de cadastro."),
  ACCOUNT_LOCKED("Sua conta está bloqueada. Por favor, contate o suporte."),
  ACCOUNT_DISABLED(
      "Sua conta está desativada e será deletada em breve. Entre em contato com o suporte para reativá-la."),
  ACCOUNT_NOT_PENDING_VERIFICATION(
      "Esta operação só pode ser executada em contas com verificação pendente."),
  ACCOUNT_NOT_ACTIVE("Esta operação só pode ser executada em contas ativas."),
  ACCOUNT_NOT_DISABLED("Esta operação só pode ser executada em contas desativadas."),
  ACCOUNT_NOT_LOCKED("Esta operação só pode ser executada em contas bloqueadas."),

  USER_NOT_FOUND("Usuário não encontrado."),
  USERNAME_ALREADY_EXISTS("Esse nome de usuário já está em uso."),
  PHONE_ALREADY_EXISTS("Esse número de telefone já está em uso."),

  USERNAME_REQUIRED("O nome de usuário é obrigatório."),
  USERNAME_INVALID_FORMAT("O nome de usuário pode conter apenas letras, números e underscore (_)."),
  USERNAME_INVALID_LENGTH("O nome de usuário deve ter entre 3 e 50 caracteres."),

  PASSWORD_REQUIRED("A senha é obrigatória."),
  PASSWORD_INVALID_LENGTH("A senha deve ter entre 6 e 20 caracteres."),
  PASSWORD_NO_WHITESPACE("A senha não pode conter espaços em branco."),
  CONFIRM_PASSWORD_REQUIRED("A confirmação de senha é obrigatória."),
  PASSWORD_HASH_REQUIRED("O hash da senha é obrigatório."),
  PASSWORD_CURRENT_INCORRECT("A Senha atual está incorreta."),
  PASSWORDS_DO_NOT_MATCH("A senha de confirmação não corresponde à senha."),

  PHONE_REQUIRED("O número de telefone é obrigatório."),
  PHONE_INVALID_FORMAT("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos."),
  PHONE_CHANGE_NOT_INITIATED(
      "Sua solicitação de alteração de telefone não foi encontrada ou já expirou. Por favor, inicie o processo novamente."),

  FULL_NAME_REQUIRED("O nome completo é obrigatório."),
  FULL_NAME_INVALID_LENGTH("O nome completo deve ter entre 3 e 100 caracteres."),

  OTP_CODE_REQUIRED("O código de verificação é obrigatório."),
  OTP_CODE_INVALID_OR_EXPIRED("Código de verificação inválido ou expirado."),
  OTP_CODE_INVALID_FORMAT("O código de verificação deve ser composto por 6 dígitos numéricos."),

  OTP_SESSION_ID_REQUIRED("O ID da sessão OTP é obrigatório."),
  OTP_SESSION_ID_INVALID_FORMAT("O formato do ID da sessão OTP é inválido."),
  OTP_SESSION_INVALID_OR_EXPIRED("Sessão para a validação do código OTP expirada ou inválida."),

  RESET_TOKEN_REQUIRED("O token de redefinição de senha é obrigatório."),
  RESET_TOKEN_INVALID_FORMAT("O formato do token de redefinição de senha é inválido."),
  RESET_TOKEN_INVALID_OR_EXPIRED("O token de redefinição de senha é inválido ou expirou."),

  REFRESH_TOKEN_REQUIRED("O token de atualização é obrigatório."),
  REFRESH_TOKEN_INVALID_FORMAT("O formato do token de atualização é inválido."),
  REFRESH_TOKEN_NOT_FOUND("Sua sessão expirou. Por favor, faça login novamente."),
  REFRESH_TOKEN_INVALID_OR_EXPIRED("Sua sessão expirou. Por favor, faça login novamente.");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
