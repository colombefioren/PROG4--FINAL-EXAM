package org.cocojojo.mg.endpoint.rest.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.config.Customizer.withDefaults;

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
                        "/view/**",
                        "/error",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(
                        PUT,
                        "/students/**",
                        "/teachers/**",
                        "/admins/**",
                        "/promotions/**",
                        "/groups/**")
                    .hasRole("ADMIN")
                    .requestMatchers(GET, "/students", "/teachers", "/promotions/*/graduates/xlsx")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(
                        PUT,
                        "/course-assignments/**",
                        "/course-assignments/*/exams",
                        "/exams/*/grades")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
        .httpBasic(withDefaults())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
