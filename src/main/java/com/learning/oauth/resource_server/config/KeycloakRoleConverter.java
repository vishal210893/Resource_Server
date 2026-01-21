package com.learning.oauth.resource_server.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Converter that extracts Keycloak realm roles from JWT tokens and converts them to Spring Security authorities.
 * <p>
 * This converter extracts roles from the {@code realm_access.roles} claim in the JWT token
 * and converts them to {@link GrantedAuthority} instances with the {@code ROLE_} prefix.
 * </p>
 *
 * <h3>JWT Structure Example:</h3>
 * <pre>
 * {
 *   "realm_access": {
 *     "roles": ["admin", "user"]
 *   }
 * }
 * </pre>
 *
 * <h3>Resulting Authorities:</h3>
 * <ul>
 *     <li>{@code ROLE_admin}</li>
 *     <li>{@code ROLE_user}</li>
 * </ul>
 *
 * @see Converter
 * @see GrantedAuthority
 * @see Jwt
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Converts JWT token to a collection of granted authorities based on Keycloak realm roles.
     * <p>
     * This method extracts roles from the {@code realm_access.roles} claim and prefixes each role
     * with {@code ROLE_} to create Spring Security authorities.
     * </p>
     *
     * @param jwt the JWT token containing the realm_access claim (must not be null)
     * @return collection of granted authorities with ROLE_ prefix, or empty collection if no roles found
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);

        if (realmAccess == null || realmAccess.isEmpty()) {
            return Collections.emptyList();
        }

        Object rolesObject = realmAccess.get(ROLES_CLAIM);
        if (!(rolesObject instanceof List)) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) rolesObject;

        return roles.stream()
                .filter(role -> role != null && !role.trim().isEmpty())
                .map(roleName -> ROLE_PREFIX + roleName)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}