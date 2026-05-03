package com.cts.fundtrack.disbursement.security;

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
 * Servlet filter that establishes Spring Security authentication context from
 * headers injected by the upstream API Gateway.
 * <p>
 * In the FundTrack microservice architecture, JWT validation is performed centrally
 * at the API Gateway. Downstream services (including this disbursement service) receive
 * pre-validated identity information via two trusted request headers:
 * <ul>
 *   <li>{@code X-User-Id} — the authenticated user's UUID</li>
 *   <li>{@code X-User-Roles} — the user's granted role (e.g., {@code FINANCE_OFFICER})</li>
 * </ul>
 * This filter reads those headers and populates the Spring Security
 * {@link org.springframework.security.core.context.SecurityContext} with a
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken},
 * enabling {@code @PreAuthorize} role checks to function correctly on controller methods.
 * </p>
 * <p>
 * If either header is absent (e.g., for public endpoints), the filter passes the
 * request through without establishing an authentication context.
 * </p>
 */
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    /**
     * Extracts identity headers from the incoming request and populates the Spring
     * Security context if both {@code X-User-Id} and {@code X-User-Roles} are present.
     * <p>
     * The role value is prefixed with {@code ROLE_} if not already present, as required
     * by Spring Security's {@code hasRole()} expression evaluator.
     * </p>
     *
     * @param request     the incoming {@link HttpServletRequest}
     * @param response    the outgoing {@link HttpServletResponse}
     * @param filterChain the remaining filter chain to continue processing
     * @throws ServletException if a servlet error occurs during filter execution
     * @throws IOException      if an I/O error occurs during filter execution
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Roles");

        if (userId != null && userRole != null) {
            // Ensure role starts with ROLE_ for Spring Security hasRole() to work
            String roleWithPrefix = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;
            
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));

            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}