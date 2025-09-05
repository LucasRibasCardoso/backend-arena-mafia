package com.projetoExtensao.arenaMafia.integration.config;

import static io.restassured.RestAssured.given;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.TokenResponseDto;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

public abstract class WebIntegrationTestConfig extends BaseTestContainersConfig {

  @LocalServerPort private int port;

  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private UserJpaRepository userJpaRepository;

  public final String defaultUsername = "test_user";
  public final String defaultPassword = "123456";
  public final String defaultFullName = "Usuário de Teste";
  public final String defaultPhone = "+558320548186";

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
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

    String accessToken = loginResponse.as(TokenResponseDto.class).accessToken();
    Cookie refreshTokenCookie = loginResponse.getDetailedCookie("refreshToken");
    RefreshTokenVO refreshToken = RefreshTokenVO.fromString(refreshTokenCookie.getValue());
    return new AuthTokensTest(accessToken, refreshToken, refreshTokenCookie);
  }

  public User mockPersistUser() {
    return mockPersistUser(AccountStatus.ACTIVE);
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

  public void deleteMockUser(UUID uuid) {
    userRepository.findById(uuid).ifPresent(user -> userJpaRepository.deleteById(user.getId()));
  }

  public void alterAccountStatus(String userPhone, AccountStatus status) {
    User user =
        userRepository
            .findByPhone(userPhone)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Usuário de teste não encontrado com o telefone: " + userPhone));

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
