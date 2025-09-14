package com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto;

/**
 * DTO que encapsula o erro de um campo de validação específico.
 *
 * @param fieldName O nome do campo que falhou na validação.
 * @param errorCode Um código de erro estável para o campo, para ser usado pelo frontend.
 * @param developerMessage Uma mensagem detalhada para o desenvolvedor.
 */
public record FieldErrorResponseDto(String fieldName, String errorCode, String developerMessage) {}
