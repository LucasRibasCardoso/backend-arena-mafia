package com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto;

/**
 * DTO que encapsula o erro de um campo de validação específico.
 *
 * @param fieldName O nome do campo que falhou na validação.
 * @param message A mensagem de erro para o campo.
 */
public record FieldErrorResponseDto(String fieldName, String message) {}
