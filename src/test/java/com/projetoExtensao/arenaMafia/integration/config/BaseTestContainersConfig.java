package com.projetoExtensao.arenaMafia.integration.config;

import static io.restassured.RestAssured.given;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.AuthResponseDto;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseTestContainersConfig {

  @Container
  private static final PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  private static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RedisTemplate<String, String> redisTemplate;
  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;

  public final String defaultUsername = "test_user";
  public final String defaultPassword = "123456";
  public final String defaultFullName = "Usuário de Teste";
  public final String defaultPhone = "+558320548186";

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

    // Redis
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @AfterEach
  void cleanupAfterEach() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, "tb_refresh_token", "tb_users");
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  public record AuthTokensTest(
      String accessToken, RefreshTokenVO refreshToken, Cookie refreshTokenCookie) {}

  public AuthTokensTest mockLogin(String username, String password) {
    LoginRequestDto loginRequest = new LoginRequestDto(username, password);
    Response loginResponse =
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String accessToken = loginResponse.as(AuthResponseDto.class).accessToken();
    Cookie refreshTokenCookie = loginResponse.getDetailedCookie("refreshToken");
    RefreshTokenVO refreshToken = RefreshTokenVO.fromString(refreshTokenCookie.getValue());
    return new AuthTokensTest(accessToken, refreshToken, refreshTokenCookie);
  }

  public User mockPersistUser() {
    return mockPersistUser(AccountStatus.ACTIVE);
  }

  public RefreshToken mockPersistRefreshToken(Long expirationTime, User user) {
    RefreshToken refreshToken = RefreshToken.create(expirationTime, user);
    return refreshTokenRepository.save(refreshToken);
  }

  public User mockPersistUser(AccountStatus status) {
    String passwordEncoded = passwordEncoder.encode(defaultPassword);
    Instant now = Instant.now();
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            defaultUsername,
            defaultFullName,
            defaultPhone,
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            now,
            now);
    return userRepository.save(user);
  }

  public User mockPersistUser(String username, String fullName, String phone, String password) {
    String passwordEncoded = passwordEncoder.encode(password);
    Instant now = Instant.now();
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            username,
            fullName,
            phone,
            passwordEncoded,
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            now,
            now);
    return userRepository.save(user);
  }

  public User mockPersistUser(
      String username, String phone, AccountStatus status, Instant createdAt, Instant updatedAt) {
    String passwordEncoded = passwordEncoder.encode("123456");
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            username,
            "Test User",
            phone,
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            createdAt,
            updatedAt);
    return userRepository.save(user);
  }

  public void deleteMockUser(UUID uuid) {
    userRepository.findById(uuid).ifPresent(user -> userJpaRepository.deleteById(user.getId()));
  }

  public void alterAccountStatus(UUID userId, AccountStatus status) {
    User user = userRepository.findById(userId).orElseThrow();

    User lockedUser =
        User.reconstitute(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getPhone(),
            user.getPasswordHash(),
            status,
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt());
    userRepository.save(lockedUser);
  }
}
