package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${spring.security.jwt.secret-key}")
  private String secretKey;

  @Value("${spring.security.jwt.access-token-expiration-ms}")
  private Long expirationMs;

  @Value("${spring.security.jwt.issuer}")
  private String issuerUrl;

  private Algorithm algorithm;

  private final UserDetailsService userDetailsService;

  public JwtTokenProvider(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @PostConstruct
  public void init() {
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    algorithm = Algorithm.HMAC256(secretKey.getBytes());
  }

  public String generateAccessToken(String username, RoleEnum role) {
    Instant now = Instant.now();
    Instant expirationAt = now.plusSeconds(expirationMs / 1000);

    return JWT.create()
        .withClaim("role", role.name())
        .withIssuedAt(now)
        .withExpiresAt(expirationAt)
        .withSubject(username)
        .withIssuer(issuerUrl)
        .sign(algorithm);
  }

  public Authentication getAuthentication(String token) {
    DecodedJWT decoder = decodedToken(token);
    UserDetails user = userDetailsService.loadUserByUsername(decoder.getSubject());
    return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
  }

  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (tokenContainsBearer(bearerToken)) {
      // retorna o token sem o prefixo "Bearer "
      return bearerToken.substring("Bearer ".length());
    }
    return null;
  }

  public boolean validateToken(String token) {
    try {
      DecodedJWT decodedJWT = decodedToken(token);
      return !decodedJWT.getExpiresAt().before(new Date());
    } catch (JWTVerificationException e) {
      throw new JWTVerificationException("Expired or invalid JWT token");
    }
  }

  private DecodedJWT decodedToken(String token) {
    // Cria um verificador de JWT com o algoritmo de decodificação
    JWTVerifier verifier = JWT.require(algorithm).build();

    // Verifica o token JWT e retorna o token decodificado
    return verifier.verify(token);
  }

  private boolean tokenContainsBearer(String token) {
    return token != null && token.startsWith("Bearer ");
  }
}
