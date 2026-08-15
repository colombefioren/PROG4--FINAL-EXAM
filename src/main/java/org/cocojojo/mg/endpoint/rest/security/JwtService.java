package org.cocojojo.mg.endpoint.rest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.cocojojo.mg.model.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey secretKey;
  private final long expirationMs;

  public JwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationMs) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(UUID userId, String email, Role role) {
    var now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("email", email)
        .claim("role", role.name())
        .issuedAt(new Date(now))
        .expiration(new Date(now + expirationMs))
        .signWith(secretKey)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
