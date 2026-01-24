package com.learning.oauth.resource_server.controller;

import com.learning.oauth.resource_server.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user-related operations.
 * <p>
 * This controller handles HTTP requests to the {@code /users} endpoint and demonstrates
 * Spring Security's OAuth2 resource server configuration with JWT token validation.
 * </p>
 *
 * <h3>Security Configuration:</h3>
 * <p>
 * Access to endpoints in this controller is secured using Spring Security with JWT-based authentication.
 * The security rules are defined in {@link com.learning.oauth.resource_server.config.SecurityConfig}.
 * </p>
 *
 * <h3>Understanding hasRole() vs hasAuthority():</h3>
 * <ul>
 *     <li><b>hasRole("developer")</b> - Internally adds {@code ROLE_} prefix, expects authority {@code ROLE_developer}</li>
 *     <li><b>hasAuthority("developer")</b> - Uses exact string, expects authority {@code developer}</li>
 *     <li><b>hasAuthority("ROLE_developer")</b> - Uses exact string, expects authority {@code ROLE_developer}</li>
 * </ul>
 *
 * <h3>JWT Token Roles and Authorities:</h3>
 * <p>
 * For Keycloak JWT tokens, roles are typically found in {@code realm_access.roles}:
 * </p>
 * <pre>
 * "realm_access": {
 *   "roles": ["developer", "offline_access", "uma_authorization"]
 * }
 * </pre>
 * <p>
 * <b>Without Converter:</b> Spring Security creates authorities directly from scopes with {@code SCOPE_} prefix.
 * To access roles, you must use {@code hasAuthority("SCOPE_profile")} for scope-based authorization.
 * </p>
 * <p>
 * <b>With KeycloakRoleConverter:</b> The custom converter extracts roles from {@code realm_access.roles}
 * and adds {@code ROLE_} prefix. This allows using {@code hasRole("developer")} which matches {@code ROLE_developer}.
 * </p>
 *
 * <h3>Scope vs Role Authorization:</h3>
 * <ul>
 *     <li><b>Scopes:</b> Spring adds {@code SCOPE_} prefix automatically (e.g., "profile" becomes {@code SCOPE_profile})</li>
 *     <li><b>Roles:</b> Converter adds {@code ROLE_} prefix (e.g., "developer" becomes {@code ROLE_developer})</li>
 *     <li>Use {@code .hasAnyAuthority("SCOPE_profile")} for scope-based checks</li>
 *     <li>Use {@code .hasRole("developer")} for role-based checks (with converter)</li>
 *     <li>Use {@code .hasAuthority("ROLE_developer")} for explicit role authority checks</li>
 * </ul>
 *
 * <h3>Example Security Configuration:</h3>
 * <pre>
 * // Require 'profile' scope for GET /users
 * .requestMatchers(HttpMethod.GET, "/users").hasAnyAuthority("SCOPE_profile")
 *
 * // Require 'developer' role for GET /users/status (with KeycloakRoleConverter)
 * .requestMatchers(HttpMethod.GET, "/users/status").hasRole("developer")
 *
 * // Alternative: explicit authority check
 * .requestMatchers(HttpMethod.GET, "/users/status").hasAuthority("ROLE_developer")
 * </pre>
 *
 * @see com.learning.oauth.resource_server.config.SecurityConfig
 * @see com.learning.oauth.resource_server.config.KeycloakRoleConverter
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private Environment environment;

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        String port = environment.getProperty("local.server.port");
        return ResponseEntity.ok("Working Resource Server on port: " + port);
    }

    @Secured("ROLE_developer")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        return ResponseEntity.ok("User deleted successfully " + id);
    }

    @PreAuthorize("hasAuthority('ROLE_developer') or #name == #jwt.subject")
    @GetMapping("/{name}")
    public ResponseEntity<String> getName(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok("Hello World " + jwt.getSubject());
    }

    @PostAuthorize("returnObject.body.userId == #jwt.subject")
    @GetMapping("/audience")
    public ResponseEntity<User> getEmailFromJwt(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new User(jwt.getClaimAsString("name"), jwt.getSubject()));
    }


}
