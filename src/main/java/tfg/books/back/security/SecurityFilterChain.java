package tfg.books.back.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityFilterChain {

  private final TokenAuthenticationFilter tokenAuthenticationFilter;

  public SecurityFilterChain(TokenAuthenticationFilter tokenAuthenticationFilter) {
      this.tokenAuthenticationFilter = tokenAuthenticationFilter;
  }

@Bean
public DefaultSecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers("/graphql"))
      .authorizeHttpRequests(authManager -> authManager
        .requestMatchers("/graphql")
        .permitAll()
        .anyRequest().denyAll());

      http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
}
