package com.projetoExtensao.arenaMafia.integration.config.util;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class TestDataFactory {

  private TestDataFactory() {}

  public static Stream<Arguments> invalidResetTokenProvider() {
    return Stream.of(
        Arguments.of(null, "RESET_TOKEN_REQUIRED"),
        Arguments.of("", "RESET_TOKEN_REQUIRED"),
        Arguments.of("  ", "RESET_TOKEN_REQUIRED"),
        Arguments.of("not-a-valid-uuid", "RESET_TOKEN_INVALID_FORMAT"));
  }

  public static Stream<Arguments> invalidRefreshTokenProvider() {
    return Stream.of(
        Arguments.of("", "REFRESH_TOKEN_REQUIRED"),
        Arguments.of("  ", "REFRESH_TOKEN_REQUIRED"),
        Arguments.of("not-a-valid-uuid", "REFRESH_TOKEN_INVALID_FORMAT"));
  }

  public static Stream<Arguments> invalidOtpSessionIdProvider() {
    return Stream.of(
        Arguments.of(null, "OTP_SESSION_ID_REQUIRED"),
        Arguments.of("", "OTP_SESSION_ID_REQUIRED"),
        Arguments.of("  ", "OTP_SESSION_ID_REQUIRED"),
        Arguments.of("not-a-valid-uuid", "OTP_SESSION_ID_INVALID_FORMAT"));
  }

  public static Stream<Arguments> invalidOtpCodeProvider() {
    return Stream.of(
        Arguments.of(null, "OTP_CODE_REQUIRED"),
        Arguments.of("", "OTP_CODE_REQUIRED"),
        Arguments.of("  ", "OTP_CODE_REQUIRED"),
        Arguments.of("123", "OTP_CODE_INVALID_FORMAT"),
        Arguments.of("aaabbb", "OTP_CODE_INVALID_FORMAT"),
        Arguments.of("12a4", "OTP_CODE_INVALID_FORMAT"));
  }

  public static Stream<Arguments> invalidUsernameProvider() {
    return Stream.of(
        Arguments.of(null, "USERNAME_REQUIRED"),
        Arguments.of("", "USERNAME_REQUIRED"),
        Arguments.of("  ", "USERNAME_REQUIRED"),
        Arguments.of("ab", "USERNAME_INVALID_LENGTH"),
        Arguments.of("a".repeat(51), "USERNAME_INVALID_LENGTH"),
        Arguments.of("invalid-user!", "USERNAME_INVALID_FORMAT"));
  }

  public static Stream<Arguments> invalidPasswordProvider() {
    return Stream.of(
        Arguments.of(null, "PASSWORD_REQUIRED"),
        Arguments.of("", "PASSWORD_REQUIRED"),
        Arguments.of("     ", "PASSWORD_REQUIRED"),
        Arguments.of("short", "PASSWORD_INVALID_LENGTH"),
        Arguments.of("a".repeat(21), "PASSWORD_INVALID_LENGTH"),
        Arguments.of("invalid pass", "PASSWORD_NO_WHITESPACE"));
  }

  public static Stream<Arguments> invalidFullNameProvider() {
    return Stream.of(
        Arguments.of("ab", "FULL_NAME_INVALID_LENGTH"),
        Arguments.of("a".repeat(101), "FULL_NAME_INVALID_LENGTH"));
  }

  public static Stream<Arguments> invalidPhoneProvider() {
    return Stream.of(
        Arguments.of(null, "PHONE_REQUIRED"),
        Arguments.of("", "PHONE_REQUIRED"),
        Arguments.of("  ", "PHONE_REQUIRED"),
        Arguments.of("123456", "PHONE_INVALID_FORMAT"),
        Arguments.of("+123abc456", "PHONE_INVALID_FORMAT"),
        Arguments.of("+1 (234) 567-8900", "PHONE_INVALID_FORMAT"));
  }
}
