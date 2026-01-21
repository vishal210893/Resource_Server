package com.learning.oauth.resource_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the OAuth2 Resource Server.
 * <p>
 * This configuration applies HTTP security settings to all incoming requests and web service endpoints.
 * It can be customized to target specific endpoints and HTTP methods with different authorization rules.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *     <li>Configures endpoint-specific security rules (e.g., GET /users requires 'profile' scope)</li>
 *     <li>Uses OAuth2 Resource Server with JWT token validation</li>
 *     <li>Spring Security inspects and validates access tokens to check for required authorities</li>
 *     <li>Scopes from JWT are automatically prefixed with "SCOPE_" when creating the authorities list</li>
 *     <li>Client applications do not need to include the "SCOPE_" prefix when acquiring tokens</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <p>
 * A GET request to {@code /users} will only be permitted if the JWT access token contains
 * the {@code profile} scope, which Spring Security validates as {@code SCOPE_profile} authority.
 * </p>
 *
 * @see SecurityFilterChain
 * @see org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

            http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/users/status")
                        //.hasAnyAuthority("SCOPE_profile")
                        .hasRole("developer1")
                        .requestMatchers("/admin/performance/**").permitAll()
                        .anyRequest()
                        .authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .csrf(AbstractHttpConfigurer::disable);

            return http.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure security", e);
        }
    }
}
