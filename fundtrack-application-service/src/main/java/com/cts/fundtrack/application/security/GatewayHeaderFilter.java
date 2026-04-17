package com.cts.fundtrack.application.security; // Notice the package name change

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
 * Spring Security filter that reconstructs the authenticated user context from
 * trusted HTTP headers injected by the API Gateway.
 *
 * <p>In the FundTrack microservice architecture, JWT verification is performed
 * once at the API Gateway. The gateway then forwards the authenticated user's
 * details as request headers to downstream services:
 * <ul>
 *   <li>{@code X-User-Roles} — the user's single role (e.g., {@code APPLICANT},
 *       {@code REVIEWER}, {@code APPROVER}, {@code ADMIN})</li>
 *   <li>{@code X-User-Email} — the user's email address, used as the principal
 *       name in the Spring Security context</li>
 *   <li>{@code X-User-Id} — the user's UUID, consumed directly from the
 *       {@link jakarta.servlet.http.HttpServletRequest} by service-layer code</li>
 * </ul>
 * </p>
 *
 * <p>This filter extends {@link OncePerRequestFilter} to guarantee it executes
 * exactly once per request. If either the role or email header is missing (e.g.,
 * for unauthenticated actuator requests), no {@code Authentication} object is set
 * and Spring Security's own access-denial mechanism handles the request.</p>
 *
 * <p>The role value is normalised to include the {@code ROLE_} prefix required
 * by Spring Security's {@code hasRole()} expressions if it is not already
 * present.</p>
 */
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    /**
     * Reads the {@code X-User-Roles} and {@code X-User-Email} headers from the
     * incoming request and, when both are present, populates the
     * {@link SecurityContextHolder} with a fully authenticated
     * {@link UsernamePasswordAuthenticationToken}.
     *
     * <p>The filter always calls {@code filterChain.doFilter()} to pass the
     * request down the chain regardless of whether an authentication object
     * was set, allowing Spring Security's authorisation rules to accept or
     * reject the request as configured in {@link com.cts.fundtrack.application.config.SecurityConfig}.</p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to execute after this filter
     * @throws ServletException if a servlet error occurs during filter processing
     * @throws IOException      if an I/O error occurs during filter processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String role = request.getHeader("X-User-Roles");
        String email = request.getHeader("X-User-Email");

        if (role != null && email != null) {
            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}