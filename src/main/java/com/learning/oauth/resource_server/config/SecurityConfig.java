package com.learning.oauth.resource_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the OAuth2 Resource Server.
 * <p>
 * This configuration applies HTTP security settings to all incoming requests and web service endpoints.
 * The basic HTTP security configuration can be applied to all requests or narrowed down to specific
 * web service endpoints and specific HTTP methods.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *     <li>Configures endpoint-specific security rules with granular authorization control</li>
 *     <li>Uses OAuth2 Resource Server with JWT token validation</li>
 *     <li>Integrates custom {@link KeycloakRoleConverter} to extract roles from Keycloak JWT tokens</li>
 *     <li>Supports both scope-based and role-based authorization</li>
 *     <li>Spring Security inspects and validates access tokens to verify required authorities</li>
 *     <li>Enables method-level security with {@code @PreAuthorize} and {@code @PostAuthorize}</li>
 * </ul>
 *
 * <h3>Request Matchers and Authorization:</h3>
 * <p>
 * The {@code requestMatchers()} method allows you to specify security rules for specific endpoints
 * and HTTP methods. For example:
 * </p>
 * <pre>
 * .requestMatchers(HttpMethod.GET, "/users/status")
 *     .hasRole("developer")
 * </pre>
 * <p>
 * This configuration tells Spring Security that if an HTTP GET request is sent to {@code /users/status},
 * the authenticated user must have the {@code developer} role (which translates to {@code ROLE_developer} authority).
 * If the pattern matches, Spring Security validates the provided access token and checks if the JWT
 * was authorized with the required authority.
 * </p>
 *
 * <h3>Understanding Authority Prefixes:</h3>
 * <h4>Scopes (Without Custom Converter):</h4>
 * <ul>
 *     <li>Spring Security automatically adds {@code SCOPE_} prefix to JWT scopes</li>
 *     <li>JWT scope: {@code "profile"} → Authority: {@code SCOPE_profile}</li>
 *     <li>Usage: {@code .hasAnyAuthority("SCOPE_profile")}</li>
 *     <li>Client applications do NOT include the {@code SCOPE_} prefix when acquiring tokens</li>
 * </ul>
 *
 * <h4>Roles (With KeycloakRoleConverter):</h4>
 * <ul>
 *     <li>Custom converter extracts roles from {@code realm_access.roles} claim</li>
 *     <li>Converter adds {@code ROLE_} prefix to each role</li>
 *     <li>JWT role: {@code "developer"} → Authority: {@code ROLE_developer}</li>
 *     <li>Usage: {@code .hasRole("developer")} (Spring adds ROLE_ prefix internally)</li>
 *     <li>Alternative: {@code .hasAuthority("ROLE_developer")} (explicit prefix)</li>
 * </ul>
 *
 * <h3>hasRole() vs hasAuthority():</h3>
 * <table border="1">
 *     <tr>
 *         <th>Method</th>
 *         <th>Prefix Behavior</th>
 *         <th>Expected Authority</th>
 *         <th>Usage Example</th>
 *     </tr>
 *     <tr>
 *         <td>{@code hasRole("developer")}</td>
 *         <td>Adds ROLE_ prefix automatically</td>
 *         <td>ROLE_developer</td>
 *         <td>For role-based authorization</td>
 *     </tr>
 *     <tr>
 *         <td>{@code hasAuthority("developer")}</td>
 *         <td>No prefix, exact match</td>
 *         <td>developer</td>
 *         <td>Won't work without converter</td>
 *     </tr>
 *     <tr>
 *         <td>{@code hasAuthority("ROLE_developer")}</td>
 *         <td>No prefix, exact match</td>
 *         <td>ROLE_developer</td>
 *         <td>Explicit role authority</td>
 *     </tr>
 *     <tr>
 *         <td>{@code hasAuthority("SCOPE_profile")}</td>
 *         <td>No prefix, exact match</td>
 *         <td>SCOPE_profile</td>
 *         <td>For scope-based authorization</td>
 *     </tr>
 * </table>
 *
 * <h3>Why Use KeycloakRoleConverter:</h3>
 * <ul>
 *     <li><b>Without Converter:</b> Spring only processes scopes from JWT, not Keycloak roles</li>
 *     <li><b>With Converter:</b> Extracts roles from {@code realm_access.roles} and adds ROLE_ prefix</li>
 *     <li>Enables use of {@code .hasRole("developer")} instead of scope-based authorization</li>
 *     <li>Aligns with Spring Security's role-based authorization pattern</li>
 * </ul>
 *
 * <h3>Example JWT Token:</h3>
 * <pre>
 * {
 *   "realm_access": {
 *     "roles": ["developer", "offline_access", "uma_authorization"]
 *   },
 *   "scope": "openid profile email"
 * }
 * </pre>
 * <p>
 * Converter creates authorities: {@code ROLE_developer}, {@code ROLE_offline_access}, {@code ROLE_uma_authorization}
 * </p>
 *
 * <h3>Configuration Examples:</h3>
 * <pre>
 * // Scope-based authorization (without converter)
 * .requestMatchers(HttpMethod.GET, "/users").hasAnyAuthority("SCOPE_profile")
 *
 * // Role-based authorization (with KeycloakRoleConverter)
 * .requestMatchers(HttpMethod.GET, "/users/status").hasRole("developer")
 *
 * // Alternative explicit authority
 * .requestMatchers(HttpMethod.GET, "/users/status").hasAuthority("ROLE_developer")
 *
 * // Public endpoints
 * .requestMatchers("/admin/performance/**").permitAll()
 * </pre>
 *
 * @see SecurityFilterChain
 * @see JwtAuthenticationConverter
 * @see KeycloakRoleConverter
 * @see EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    /**
     * Converter that maps Keycloak realm roles from JWT into Spring Security authorities.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    /**
     * Configures the security filter chain for HTTP requests.
     * <p>
     * This method creates and configures a {@link SecurityFilterChain} that defines how incoming
     * HTTP requests are secured. It sets up JWT token validation, role-based authorization,
     * and endpoint-specific security rules.
     * </p>
     *
     * <h3>Configuration Steps:</h3>
     * <ol>
     *     <li>Creates {@link JwtAuthenticationConverter} to convert JWT tokens to Spring Security authorities</li>
     *     <li>Registers {@link KeycloakRoleConverter} to extract roles from {@code realm_access.roles} claim</li>
     *     <li>Configures HTTP request authorization rules for specific endpoints and methods</li>
     *     <li>Sets up OAuth2 Resource Server with JWT token validation</li>
     *     <li>Disables CSRF protection (common for stateless REST APIs)</li>
     * </ol>
     *
     * <h3>Authorization Rules:</h3>
     * <ul>
     *     <li><b>GET /users/status:</b> Requires {@code developer} role (ROLE_developer authority)</li>
     *     <li><b>/admin/performance/**:</b> Publicly accessible (permitAll)</li>
     *     <li><b>All other requests:</b> Must be authenticated with valid JWT token</li>
     * </ul>
     *
     * <h3>JWT Authentication Flow:</h3>
     * <ol>
     *     <li>Client sends request with JWT access token in Authorization header</li>
     *     <li>Spring Security validates JWT signature, expiration, and issuer</li>
     *     <li>{@link KeycloakRoleConverter} extracts roles from {@code realm_access.roles}</li>
     *     <li>Each role is prefixed with {@code ROLE_} (e.g., "developer" → "ROLE_developer")</li>
     *     <li>Spring Security checks if user has required authorities for the endpoint</li>
     *     <li>If authorized, request proceeds; otherwise, returns 403 Forbidden</li>
     * </ol>
     *
     * <h3>Why hasRole("developer") Works:</h3>
     * <p>
     * The {@code .hasRole("developer")} method internally adds the {@code ROLE_} prefix,
     * so it checks for {@code ROLE_developer} authority. Since {@link KeycloakRoleConverter}
     * extracts "developer" from the JWT and prefixes it with {@code ROLE_}, the authorization
     * succeeds when the JWT contains:
     * </p>
     * <pre>
     * "realm_access": {
     *   "roles": ["developer"]
     * }
     * </pre>
     *
     * <h3>Alternative Configurations:</h3>
     * <pre>
     * // Using explicit authority (equivalent to hasRole("developer"))
     * .hasAuthority("ROLE_developer")
     *
     * // Using scope-based authorization (without custom converter)
     * .hasAnyAuthority("SCOPE_profile")
     *
     * // Multiple roles
     * .hasAnyRole("developer", "admin")
     * </pre>
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws RuntimeException if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        //.hasAnyAuthority("SCOPE_profile")
                        .requestMatchers(HttpMethod.GET, "/users/status")
                        .hasRole("developer")
                        .requestMatchers("/admin/performance/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }
}
