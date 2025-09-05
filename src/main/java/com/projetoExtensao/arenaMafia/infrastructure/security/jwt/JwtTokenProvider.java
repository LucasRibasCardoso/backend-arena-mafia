package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidJwtTokenException;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.CustomUserDetailsService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

  private final CustomUserDetailsService customUserDetailsService;

  public JwtTokenProvider(CustomUserDetailsService customUserDetailsService) {
    this.customUserDetailsService = customUserDetailsService;
  }

  @PostConstruct
  public void init() {
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    algorithm = Algorithm.HMAC256(secretKey.getBytes());
  }

  public String generateAccessToken(UUID userId, String username, RoleEnum role) {
    Instant now = Instant.now();
    Instant expirationAt = now.plusSeconds(expirationMs / 1000);

    return JWT.create()
        .withSubject(userId.toString())
        .withClaim("username", username)
        .withClaim("role", role.name())
        .withIssuedAt(now)
        .withExpiresAt(expirationAt)
        .withIssuer(issuerUrl)
        .sign(algorithm);
  }

  public Authentication getAuthentication(String token) {
    try {
      DecodedJWT decoder = decodedToken(token);
      UUID userId = UUID.fromString(decoder.getSubject());
      UserDetails user = customUserDetailsService.loadUserById(userId);
      return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    } catch (JWTVerificationException e) {
      throw new InvalidJwtTokenException("Token JWT inválido ou expirado.");
    }
  }

  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (tokenContainsBearer(bearerToken)) {
      // retorna o token sem o prefixo "Bearer "
      return bearerToken.substring("Bearer ".length());
    }
    return null;
  }

  private DecodedJWT decodedToken(String token) {
    JWTVerifier verifier = JWT.require(algorithm).build();
    return verifier.verify(token);
  }

  private boolean tokenContainsBearer(String token) {
    return token != null && token.startsWith("Bearer ");
  }
}
