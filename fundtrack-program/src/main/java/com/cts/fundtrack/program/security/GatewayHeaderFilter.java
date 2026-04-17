package com.cts.fundtrack.program.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that converts API Gateway-injected HTTP headers into a Spring Security
 * {@link org.springframework.security.core.Authentication} object for each incoming request.
 *
 * <p>In the FundTrack microservice architecture, the API Gateway is responsible for
 * authenticating users (via JWT validation) before forwarding requests to downstream
 * services. Once authenticated, the Gateway strips the original Authorization header
 * and replaces it with trusted internal headers carrying the verified user identity:</p>
 * <ul>
 *   <li>{@code X-User-Roles} — the user's role name (e.g., {@code "ADMIN"},
 *       {@code "APPLICANT"}). Spring Security's {@code hasRole()} check expects the
 *       {@code "ROLE_"} prefix, so this filter prepends it automatically.</li>
 *   <li>{@code X-User-Email} — the authenticated user's email address, used as the
 *       principal name in the security context.</li>
 * </ul>
 *
 * <p>If both headers are present, a {@link UsernamePasswordAuthenticationToken} is
 * constructed with the email as principal, no credentials, and a single granted authority
 * derived from the role header. This token is stored in the
 * {@link SecurityContextHolder} for the duration of the request, enabling
 * {@code @PreAuthorize} expressions and other method-security checks to function
 * correctly.</p>
 *
 * <p>If either header is absent (e.g., for public/actuator endpoints), the filter
 * passes the request through without setting any authentication, leaving the security
 * context empty and allowing Spring Security's access rules to handle the unauthenticated
 * request (typically resulting in a {@code 403 Forbidden} for protected endpoints).</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee the filter logic executes exactly
 * once per HTTP request, even in servlet environments that may dispatch requests
 * internally multiple times.</p>
 */
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    /**
     * Extracts gateway-injected identity headers and populates the Spring Security context.
     *
     * <p>For each request, this method reads the {@code X-User-Roles} and
     * {@code X-User-Email} headers. If both are present, a fully authenticated
     * {@link UsernamePasswordAuthenticationToken} is built with the email as the
     * principal and a single {@link SimpleGrantedAuthority} of the form
     * {@code "ROLE_" + role}. The token is then set on the
     * {@link SecurityContextHolder} before the request continues down the filter chain.</p>
     *
     * @param request     the incoming HTTP request, providing access to gateway headers.
     * @param response    the HTTP response object, passed through unmodified by this filter.
     * @param filterChain the remaining filter chain; {@code doFilter} is always called
     *                    to ensure the request reaches its target handler.
     * @throws ServletException if the filter chain throws a servlet-level exception.
     * @throws IOException      if an I/O error occurs during filter chain processing.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract values injected by your API Gateway
        String role = request.getHeader("X-User-Roles");  // Gateway sends "X-User-Roles" with value "ADMIN"
        String email = request.getHeader("X-User-Email");

        if (role != null && email != null) {
            // Gateway sends plain role name e.g. "ADMIN"; Spring's hasRole() checks for "ROLE_ADMIN"
            List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

            // 3. Create the internal Authentication object
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

            // 4. Set it in the Security Context for this specific request
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
