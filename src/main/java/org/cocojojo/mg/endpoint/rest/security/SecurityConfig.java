package org.cocojojo.mg.endpoint.rest.security;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final RequestMappingHandlerMapping handlerMapping;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/auth/**",
                        "/ping",
                        "/health/**",
                        "/error",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico")
                    .permitAll()
                    .requestMatchers("/ui/login", "/ui/logout", "/ui/not-authorized")
                    .permitAll()
                    .requestMatchers("/ui/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        PUT,
                        "/students/**",
                        "/teachers/**",
                        "/admins/**",
                        "/promotions/**",
                        "/groups/**",
                        "/courses/**")
                    .hasRole("ADMIN")
                    .requestMatchers(GET, "/admins/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        GET,
                        "/students",
                        "/teachers",
                        "/teachers/*",
                        "/groups",
                        "/groups/*/students",
                        "/courses",
                        "/courses/*",
                        "/promotions/*/courses")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(
                        GET, "/students/*", "/students/*/group-flows", "/students/*/grades")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(GET, "/promotions", "/promotions/*")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(GET, "/grades/*")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(POST, "/students/*/yearly-results/*/transcript")
                    .authenticated()
                    .requestMatchers(GET, "/students/*/yearly-results/*")
                    .authenticated()
                    .requestMatchers(GET, "/students/*/results-summary")
                    .authenticated()
                    .requestMatchers(
                        GET,
                        "/promotions/*/graduates",
                        "/promotions/*/graduates/export",
                        "/promotions/*/graduates/download")
                    .hasRole("ADMIN")
                    .requestMatchers(GET, "/course-assignments/curriculum-status")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        GET,
                        "/course-assignments",
                        "/course-assignments/*",
                        "/course-assignments/*/exams",
                        "/course-assignments/*/exams/*")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(
                        GET, "/exams/*/grades", "/exams/*/students/*/grade", "/grades/*/history")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(DELETE, "/grades/*")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(PATCH, "/exams/*/students/*/grade")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(PUT, "/course-assignments/*/exams", "/exams/*/grades")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(PUT, "/course-assignments/**")
                    .hasRole("ADMIN")
                    .requestMatchers(DELETE, "/course-assignments/*/exams/**")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(DELETE, "/course-assignments/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (request, response, authException) -> {
                      // A request that does not map to any controller is a non-existent endpoint,
                      // so answer 404 rather than leaking an auth challenge for it.
                      boolean mapsToHandler;
                      try {
                        mapsToHandler = handlerMapping.getHandler(request) != null;
                      } catch (Exception ignored) {
                        mapsToHandler = true;
                      }
                      if (!mapsToHandler) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                      }
                      // Keep the WWW-Authenticate header so browsers prompt for credentials
                      // when opening the /ui pages without a bearer token.
                      response.setHeader("WWW-Authenticate", "Basic realm=\"hei\"");
                      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    }))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
