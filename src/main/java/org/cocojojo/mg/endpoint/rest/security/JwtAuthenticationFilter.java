package org.cocojojo.mg.endpoint.rest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /**
   * Cookie set by the /ui/login form so browsers can authenticate UI pages without a bearer header.
   */
  public static final String TOKEN_COOKIE = "hei_token";

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var token = bearerToken(request);
    if (token.isEmpty()) {
      token = cookieToken(request);
    }
    if (token.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      var claims = jwtService.parseToken(token);
      var userId = claims.getSubject();
      var email = claims.get("email", String.class);
      var role = claims.get("role", String.class);

      var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
      var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
      authentication.setDetails(email);

      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private String bearerToken(HttpServletRequest request) {
    var authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "";
    }
    return authHeader.substring(7);
  }

  private String cookieToken(HttpServletRequest request) {
    var cookies = request.getCookies();
    if (cookies == null) {
      return "";
    }
    return Arrays.stream(cookies)
        .filter(cookie -> TOKEN_COOKIE.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse("");
  }
}
