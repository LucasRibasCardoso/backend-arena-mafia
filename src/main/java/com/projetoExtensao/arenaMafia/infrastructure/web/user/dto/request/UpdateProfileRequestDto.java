package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
    @Size(min = 3, max = 100, message = "FULL_NAME_INVALID_LENGTH") String fullName) {}
