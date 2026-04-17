package com.cts.fundtrack.analytics.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servlet filter that reconstructs the Spring Security authentication context
 * from HTTP headers injected by the API Gateway.
 *
 * <p>The API Gateway validates the JWT and then forwards the authenticated user's
 * identity downstream via the following custom headers:</p>
 * <ul>
 *   <li>{@code X-User-Id} — the UUID of the authenticated user, used as the security principal</li>
 *   <li>{@code X-User-Roles} — the user's role (e.g., {@code ADMIN}, {@code FINANCE_OFFICER})</li>
 * </ul>
 *
 * <p>If both headers are present, a {@link UsernamePasswordAuthenticationToken} is created
 * and placed in the {@link SecurityContextHolder}, enabling {@code @PreAuthorize} checks
 * on controller methods. Requests missing these headers pass through unauthenticated
 * and will be rejected by the authorization rules defined in {@code SecurityConfig}.</p>
 *
 * <p>This filter extends {@link OncePerRequestFilter} to guarantee it executes exactly
 * once per HTTP request.</p>
 */
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    /**
     * Extracts {@code X-User-Id} and {@code X-User-Roles} headers from the incoming request
     * and populates the {@link SecurityContextHolder} with a fully authenticated token.
     *
     * <p>The role is normalized to include the {@code ROLE_} prefix required by Spring Security's
     * {@code hasRole()} checks. If either header is absent, the filter chain proceeds without
     * setting an authentication context.</p>
     *
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @param filterChain the remaining filter chain to invoke after this filter
     * @throws ServletException if the filter chain processing throws a servlet error
     * @throws IOException      if an I/O error occurs during request/response processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Roles");

        if (userId != null && userRole != null) {
            // Standardizing the role prefix for .hasRole() compatibility
            String roleWithPrefix = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;
            
            List<SimpleGrantedAuthority> authorities = 
                Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));

            // Setting the userId as the 'Principal' for later use in SecurityService
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}