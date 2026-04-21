package com.petsupplies.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.core.web.SecurityExceptionHandlers;
import com.petsupplies.core.security.Pbkdf2Sha256PasswordEncoder;
import com.petsupplies.user.service.UserLockoutService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new Pbkdf2Sha256PasswordEncoder();
  }

  @Bean
  public AuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      UserLockoutService lockoutService
  ) {
    return new AuthenticationProvider() {
      @Override
      public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = String.valueOf(authentication.getCredentials());
        String ip = null;
        if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
          ip = details.getRemoteAddress();
        }

        try {
          var userDetails = userDetailsService.loadUserByUsername(username);

          if (userDetails instanceof org.springframework.security.core.userdetails.User u) {
            // no-op: type hint
          }

          if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            lockoutService.onAuthenticationFailure(username, ip);
            throw new BadCredentialsException("Bad credentials");
          }

          lockoutService.onAuthenticationSuccess(username, ip);

          return new UsernamePasswordAuthenticationToken(userDetails, rawPassword, userDetails.getAuthorities());
        } catch (LockedException le) {
          throw le;
        }
      }

      @Override
      public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
      }
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(SecurityExceptionHandlers.jsonAuthenticationEntryPoint(objectMapper))
            .accessDeniedHandler(SecurityExceptionHandlers.jsonAccessDeniedHandler(objectMapper))
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/health").permitAll()
            .requestMatchers("/ws/**").authenticated()
            .anyRequest().authenticated()
        );

    return http.build();
  }
}

