package com.projetoExtensao.arenaMafia.integration.config.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterizedTest
@MethodSource(
    "com.projetoExtensao.arenaMafia.integration.config.util.TestDataProvider#invalidResetTokenProvider")
public @interface InvalidResetTokenProvider {}
