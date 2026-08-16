package org.cocojojo.mg.endpoint.rest.security;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.PUT;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                    .requestMatchers("/view/**")
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
                        "/courses",
                        "/courses/*",
                        "/promotions/*/graduates/xlsx")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(
                        PUT,
                        "/course-assignments/**",
                        "/course-assignments/*/exams",
                        "/exams/*/grades",
                        "/exams/*/students/*/grade")
                    .hasAnyRole("ADMIN", "TEACHER")
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
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
