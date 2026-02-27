package br.com.autevia.finkidsapi.config;

import br.com.autevia.finkidsapi.security.AutomationAuthenticationEntryPoint;
import br.com.autevia.finkidsapi.security.AutomationTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AutomationTokenAuthenticationFilter automationTokenAuthenticationFilter,
            AutomationAuthenticationEntryPoint automationAuthenticationEntryPoint
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(automationAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/automation/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(automationTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .rememberMe(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults())
                .build();
    }
}
