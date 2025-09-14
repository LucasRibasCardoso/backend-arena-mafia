package com.projetoExtensao.arenaMafia.unit.config;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class TestDataProvider {

  private TestDataProvider() {}

  public static final String defaultPhone = "+558320548181";
  public static final String defaultUsername = "testuser";
  public static final String defaultFullName = "Test User";
  public static final String defaultPassword = "password123";
  public static final RoleEnum defaultRole = RoleEnum.ROLE_USER;

  public static User createPendintUser() {
    return UserBuilder.defaultUser().withStatus(AccountStatus.PENDING_VERIFICATION).build();
  }

  public static User createActiveUser() {
    return UserBuilder.defaultUser().withStatus(AccountStatus.ACTIVE).build();
  }

  public static RefreshToken createRefreshToken(User user) {
    return RefreshToken.create(7L, user);
  }

  public static RefreshToken createExpiredRefreshToken(User user) {
    return RefreshToken.create(-7L, user);
  }

  public static class UserBuilder {
    private UUID id = UUID.randomUUID();
    private String username = defaultUsername;
    private String fullName = defaultFullName;
    private String phone = defaultPhone;
    private String passwordHash = defaultPassword;
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;
    private RoleEnum role = RoleEnum.ROLE_USER;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public static UserBuilder defaultUser() {
      return new UserBuilder();
    }

    public UserBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public UserBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public UserBuilder withFullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public UserBuilder withPhone(String phone) {
      this.phone = phone;
      return this;
    }

    public UserBuilder withStatus(AccountStatus status) {
      this.status = status;
      return this;
    }

    public UserBuilder withRole(RoleEnum role) {
      this.role = role;
      return this;
    }

    public User build() {
      return User.reconstitute(
          id, username, fullName, phone, passwordHash, status, role, createdAt, updatedAt);
    }
  }

  public static Stream<Arguments> invalidAccountStatusToRequestOtpProvider() {
    return Stream.of(
        Arguments.of(AccountStatus.LOCKED, ErrorCode.ACCOUNT_LOCKED),
        Arguments.of(AccountStatus.DISABLED, ErrorCode.ACCOUNT_DISABLED));
  }

  public static Stream<Arguments> accountStatusNonActiveProvider() {
    return Stream.of(
        Arguments.of(AccountStatus.LOCKED, ErrorCode.ACCOUNT_LOCKED),
        Arguments.of(AccountStatus.DISABLED, ErrorCode.ACCOUNT_DISABLED),
        Arguments.of(AccountStatus.PENDING_VERIFICATION, ErrorCode.ACCOUNT_PENDING_VERIFICATION));
  }

  public static Stream<Arguments> invalidUsernameProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.USERNAME_REQUIRED),
        Arguments.of("", ErrorCode.USERNAME_REQUIRED),
        Arguments.of("ab", ErrorCode.USERNAME_INVALID_LENGTH),
        Arguments.of("a".repeat(51), ErrorCode.USERNAME_INVALID_LENGTH),
        Arguments.of("invalid username", ErrorCode.USERNAME_INVALID_FORMAT));
  }

  public static Stream<Arguments> invalidFullNameProvider() {
    return Stream.of(
        Arguments.of("", ErrorCode.FULL_NAME_REQUIRED),
        Arguments.of("A", ErrorCode.FULL_NAME_INVALID_LENGTH),
        Arguments.of("A".repeat(101), ErrorCode.FULL_NAME_INVALID_LENGTH));
  }

  public static Stream<Arguments> invalidPhoneProvider() {
    return Stream.of(
        Arguments.of("", ErrorCode.PHONE_REQUIRED),
        Arguments.of("12345", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+1", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+12345678901234567", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+0123456789", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+1", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+55 83 999998888", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+55-83-999998888", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("+55(83)999998888", ErrorCode.PHONE_INVALID_FORMAT),
        Arguments.of("telefone", ErrorCode.PHONE_INVALID_FORMAT));
  }

  public static Stream<Arguments> invalidOtpCodeProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.OTP_CODE_REQUIRED),
        Arguments.of("", ErrorCode.OTP_CODE_REQUIRED),
        Arguments.of("123", ErrorCode.OTP_CODE_INVALID_FORMAT),
        Arguments.of("abcdef", ErrorCode.OTP_CODE_INVALID_FORMAT),
        Arguments.of("12345a", ErrorCode.OTP_CODE_INVALID_FORMAT),
        Arguments.of("1234567", ErrorCode.OTP_CODE_INVALID_FORMAT));
  }

  public static Stream<Arguments> invalidOtpSessionIdProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.OTP_SESSION_ID_REQUIRED),
        Arguments.of("", ErrorCode.OTP_SESSION_ID_REQUIRED),
        Arguments.of("not-a-uuid", ErrorCode.OTP_SESSION_ID_INVALID_FORMAT),
        Arguments.of("12345", ErrorCode.OTP_SESSION_ID_INVALID_FORMAT));
  }

  public static Stream<Arguments> invalidRefreshTokenProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.REFRESH_TOKEN_REQUIRED),
        Arguments.of("", ErrorCode.REFRESH_TOKEN_REQUIRED),
        Arguments.of("not-a-uuid", ErrorCode.REFRESH_TOKEN_INVALID_FORMAT),
        Arguments.of("12345", ErrorCode.REFRESH_TOKEN_INVALID_FORMAT));
  }

  public static Stream<Arguments> invalidResetTokenProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.RESET_TOKEN_REQUIRED),
        Arguments.of("", ErrorCode.RESET_TOKEN_REQUIRED),
        Arguments.of("not-a-uuid", ErrorCode.RESET_TOKEN_INVALID_FORMAT),
        Arguments.of("12345", ErrorCode.RESET_TOKEN_INVALID_FORMAT));
  }
}
