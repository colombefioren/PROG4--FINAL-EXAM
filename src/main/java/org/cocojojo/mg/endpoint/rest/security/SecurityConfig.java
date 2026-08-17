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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;

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
                        "/swagger-ui/**")
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
                        "/groups",
                        "/groups/*/students",
                        "/courses",
                        "/courses/*")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(POST, "/students/*/yearly_results/*/transcript")
                    .authenticated()
                    .requestMatchers(GET, "/students/*/yearly_results/*")
                    .authenticated()
                    .requestMatchers(
                        GET, "/promotions/*/graduates", "/promotions/*/graduates/export")
                    .hasRole("ADMIN")
                    .requestMatchers(GET, "/course-assignments/curriculum-status")
                    .hasRole("ADMIN")
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
