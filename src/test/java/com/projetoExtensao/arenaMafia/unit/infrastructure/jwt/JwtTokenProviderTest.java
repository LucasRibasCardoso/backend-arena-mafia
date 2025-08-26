package com.projetoExtensao.arenaMafia.unit.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidJwtTokenException;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para JwtTokenProvider")
public class JwtTokenProviderTest {

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private JwtTokenProvider tokenProvider;

  private String secretKey;
  private String username;

  @BeforeEach
  public void setUp() {
    secretKey = "I5fbSc1fDSiV0jRABD2hwVqn/RZweuO96QHRM+BmyoY=";
    Long expirationMs = 3600000L; // 1 hour in milliseconds
    username = "testUser";

    // Cria um mock para HttpServletRequest
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

    ReflectionTestUtils.setField(tokenProvider, "secretKey", secretKey);
    ReflectionTestUtils.setField(tokenProvider, "expirationMs", expirationMs);
    // Usa ReflectionTestUtils para invocar o metodo protegido 'init'
    ReflectionTestUtils.invokeMethod(tokenProvider, "init");
  }

  @AfterEach
  void cleanHttpContext() {
    // Limpa o contexto da requisição após cada teste.
    RequestContextHolder.resetRequestAttributes();
  }

  @Nested
  @DisplayName("Criação de Token")
  class TokenCreationTests {

    @Test
    @DisplayName("Deve retornar um token JWT válido com as informações corretas")
    void getAccessToken_shouldReturnValidTokenJwt() {
      // Arrange
      RoleEnum role = RoleEnum.ROLE_USER;

      // Act
      String tokenJWT = tokenProvider.generateAccessToken(username, role);

      // Assert
      assertThat(tokenJWT).isNotBlank();

      // Valida o conteúdo do token
      var decodedJWT = JWT.decode(tokenJWT);
      assertThat(decodedJWT.getSubject()).isEqualTo(username);
      assertThat(decodedJWT.getClaim("role").asString()).isEqualTo(role.name());
      assertThat(decodedJWT.getExpiresAt()).isAfter(new Date());
    }
  }

  @Nested
  @DisplayName("Obtenção de Autenticação")
  class AuthenticationRetrievalTests {

    @Test
    @DisplayName("Deve retornar um objeto Authentication para um token válido")
    void getAuthentication_shouldReturnAuthenticationForValidToken() {
      // Arrange
      String tokenJWT = tokenProvider.generateAccessToken(username, RoleEnum.ROLE_USER);
      UserDetails userDetails = mock(UserDetails.class);

      // Configura o comportamento do mock
      when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

      // Act
      Authentication authentication = tokenProvider.getAuthentication(tokenJWT);

      // Assert
      assertThat(authentication).isNotNull();
      assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
      verify(userDetailsService, times(1)).loadUserByUsername(username);
    }

    @Test
    @DisplayName("Deve lançar InvalidJwtTokenException para um token expirado")
    void getAuthentication_shouldThrowExceptionForExpiredToken() {
      // Arrange
      String expiredToken = createExpiredToken(username, RoleEnum.ROLE_USER);

      // Act & Assert
      assertThatThrownBy(() -> tokenProvider.getAuthentication(expiredToken))
          .isInstanceOf(InvalidJwtTokenException.class)
          .hasMessage("Token JWT inválido ou expirado.");

      // Verify
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Deve lançar InvalidJwtTokenException para um token com assinatura inválida")
    void getAuthentication_shouldThrowExceptionForInvalidSignatureToken() {
      // Arrange
      String validToken = tokenProvider.generateAccessToken(username, RoleEnum.ROLE_USER);
      String tamperedToken = validToken + "invalid-signature";

      // Act & Assert
      assertThatThrownBy(() -> tokenProvider.getAuthentication(tamperedToken))
          .isInstanceOf(InvalidJwtTokenException.class)
          .hasMessage("Token JWT inválido ou expirado.");

      // Verify
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Deve lançar InvalidJwtTokenException para um token malformado")
    void getAuthentication_shouldThrowExceptionForMalformedToken() {
      // Arrange
      String malformedToken = "not-a-valid-jwt";

      // Act & Assert
      assertThatThrownBy(() -> tokenProvider.getAuthentication(malformedToken))
          .isInstanceOf(InvalidJwtTokenException.class)
          .hasMessage("Token JWT inválido ou expirado.");

      // Verify
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
  }

  @Nested
  @DisplayName("Resolução de Token a partir da Requisição")
  class TokenResolutionTests {

    @Test
    @DisplayName("Deve extrair o token de um header 'Authorization' válido")
    void resolve_token_shouldExtractTokenFromValidHeader() {
      // Arrange
      String tokenJWT = "my-jwt-tokenJWT";
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + tokenJWT);

      // Act
      String resolvedToken = tokenProvider.resolveToken(request);

      // Assert
      assertThat(resolvedToken).isEqualTo(tokenJWT);
    }

    @Test
    @DisplayName("Deve retornar null se o header não começar com o prefixo 'Bearer '")
    void resolveToken_shouldReturnNullWhenHeaderIsInvalid() {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Basic my-jwt-token");

      // Act
      String resolvedToken = tokenProvider.resolveToken(request);

      // Assert
      assertThat(resolvedToken).isNull();
    }

    @Test
    @DisplayName("Deve retornar null se não houver header 'Authorization'")
    void resolveToken_shouldReturnNullWhenHeaderIsMissing() {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();

      // Act
      String resolvedToken = tokenProvider.resolveToken(request);

      // Assert
      assertThat(resolvedToken).isNull();
    }
  }

  private String createExpiredToken(String username, RoleEnum role) {
    Instant now = Instant.now();
    String encodedSecret = Base64.getEncoder().encodeToString(secretKey.getBytes());
    Algorithm algorithm = Algorithm.HMAC256(encodedSecret.getBytes());

    return JWT.create()
        .withClaim("role", role.name())
        .withIssuedAt(now.minusSeconds(7200)) // Criado 2 horas atrás
        .withExpiresAt(now.minusSeconds(3600)) // Expirou 1 hora atrás
        .withSubject(username)
        .sign(algorithm);
  }
}
