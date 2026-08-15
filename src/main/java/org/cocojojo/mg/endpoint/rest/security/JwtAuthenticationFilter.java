package org.cocojojo.mg.endpoint.rest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    var token = authHeader.substring(7);

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
}
