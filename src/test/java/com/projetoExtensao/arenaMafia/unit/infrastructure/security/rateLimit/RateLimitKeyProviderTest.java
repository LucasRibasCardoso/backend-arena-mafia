package com.projetoExtensao.arenaMafia.unit.infrastructure.security.rateLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.RateLimitKeyProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class RateLimitKeyProviderTest {

  @Mock private HttpServletRequest request;
  @InjectMocks private RateLimitKeyProvider keyProvider;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Deve retornar o username quando o usuário está autenticado")
  void shouldReturnUsername_whenUserIsAuthenticated() {
    // Arrange
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "testuser", null, AuthorityUtils.createAuthorityList("ROLE_USER"));

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // Act
    String key = keyProvider.resolveKey(request);

    // Assert
    assertThat(key).isEqualTo("testuser");
    verify(request, never()).getRemoteAddr();
    verify(request, never()).getHeader(anyString());
  }

  @Test
  @DisplayName("Deve retornar o IP de getRemoteAddr quando o usuário é anônimo e não há proxy")
  void shouldReturnIpFromRemoteAddr_whenUserIsAnonymous() {
    // Arrange
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");

    // Act
    String key = keyProvider.resolveKey(request);

    // Assert
    assertThat(key).isEqualTo("192.168.1.1");
  }

  @Test
  @DisplayName(
      "Deve retornar o IP do header X-Forwarded-For quando o usuário está atrás de um proxy")
  void shouldReturnIpFromXForwardedForHeader_whenBehindProxy() {
    // Arrange
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 198.51.100.2");

    // Act
    String key = keyProvider.resolveKey(request);

    // Assert
    assertThat(key).isEqualTo("203.0.113.10");
    verify(request, never()).getRemoteAddr();
  }

  @Test
  @DisplayName("Deve retornar o IP quando a autenticação é de um usuário anônimo do Spring")
  void shouldReturnIp_whenAuthenticationIsAnonymousToken() {
    // Arrange
    Authentication anonymousAuth =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(anonymousAuth);
    SecurityContextHolder.setContext(securityContext);

    when(request.getRemoteAddr()).thenReturn("10.10.10.10");

    // Act
    String key = keyProvider.resolveKey(request);

    // Assert
    assertThat(key).isEqualTo("10.10.10.10");
  }
}
