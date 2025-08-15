package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class JwtTokenProvider {

  private static final ZoneOffset UTC_ZONE = ZoneOffset.UTC;

  @Value("${spring.security.jwt.secret-key}")
  private String secretKey;

  @Value("${spring.security.jwt.expirationMs}")
  private Long expirationMs;

  private Algorithm algorithm;

  private final UserDetailsService userDetailsService;

  public JwtTokenProvider(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @PostConstruct
  protected void init() {
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    algorithm = Algorithm.HMAC256(secretKey.getBytes());
  }

  public TokenResponseDto createAccessToken(String username, RoleEnum role) {
    Instant now = Instant.now();
    Instant expirationAt = now.plusSeconds(expirationMs / 1000);
    String accessToken = getAccessToken(username, role, now, expirationAt);
    String refreshToken = getRefreshToken(username, role, now);

    return new TokenResponseDto(
        username,
        LocalDateTime.ofInstant(now, UTC_ZONE),
        LocalDateTime.ofInstant(expirationAt, UTC_ZONE),
        accessToken,
        refreshToken);
  }

  public TokenResponseDto createRefreshToken(String refreshToken) {
    if (tokenContainsBearer(refreshToken)) {
      refreshToken = refreshToken.substring("Bearer ".length());
    }
    DecodedJWT decodedJWT = decodedToken(refreshToken);

    String username = decodedJWT.getSubject();
    RoleEnum role = decodedJWT.getClaim("role").as(RoleEnum.class);

    return createAccessToken(username, role);
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
    DecodedJWT decodedJWT = decodedToken(token);
    try {
      return !decodedJWT.getExpiresAt().before(new Date());
    } catch (JWTVerificationException e) {
      throw new JWTVerificationException("Expired or invalid JWT token");
    }
  }

  private String getAccessToken(String username, RoleEnum role, Instant now, Instant expirationAt) {
    String issuerUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    return JWT.create()
        .withClaim("role", role.name())
        .withIssuedAt(now)
        .withExpiresAt(expirationAt)
        .withSubject(username)
        .withIssuer(issuerUrl)
        .sign(algorithm);
  }

  private String getRefreshToken(String username, RoleEnum role, Instant now) {
    Instant refreshTokenExpirationAt = now.plusSeconds(604800); // 7 dias
    return JWT.create()
        .withClaim("role", role.name())
        .withIssuedAt(now)
        .withExpiresAt(refreshTokenExpirationAt)
        .withSubject(username)
        .sign(algorithm);
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
