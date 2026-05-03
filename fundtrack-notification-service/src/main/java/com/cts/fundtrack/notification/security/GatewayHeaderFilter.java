package com.cts.fundtrack.notification.security;

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
 * Servlet filter that establishes a Spring Security authentication context from
 * identity headers injected by the API Gateway on every inbound request.
 *
 * <p>The Notification Service does not perform its own JWT validation. Instead, it
 * trusts the API Gateway to authenticate callers and propagate their identity via
 * two custom HTTP headers:</p>
 * <ul>
 *   <li>{@code X-User-Role} — the caller's role string, expected in
 *       {@code ROLE_<ROLE_NAME>} format (e.g., {@code ROLE_ADMIN}).</li>
 *   <li>{@code X-User-Email} — the caller's email address, used as the
 *       Spring Security principal name.</li>
 * </ul>
 *
 * <p>If both headers are present, the filter constructs a
 * {@link UsernamePasswordAuthenticationToken} and places it into the
 * {@link SecurityContextHolder} for the duration of the current request. If either
 * header is absent the filter passes the request through unchanged, and Spring
 * Security's access-control rules will reject any protected endpoint.</p>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee exactly one execution
 * per request regardless of how many filter chains are active.</p>
 *
 * @see com.cts.fundtrack.notification.config.SecurityConfig
 */
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    /**
     * Core filter logic that reads gateway-injected identity headers and
     * populates the {@link SecurityContextHolder} for the current request.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Read {@code X-User-Role} and {@code X-User-Email} from the request headers.</li>
     *   <li>If both are present, wrap them in a {@link UsernamePasswordAuthenticationToken}
     *       with the role as a {@link SimpleGrantedAuthority}.</li>
     *   <li>Set the authentication on the current {@link SecurityContextHolder}.</li>
     *   <li>Continue the filter chain unconditionally.</li>
     * </ol>
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain to execute
     * @throws ServletException if a servlet-level error occurs
     * @throws IOException      if an I/O error occurs while processing the request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract values injected by your API Gateway
        String role = request.getHeader("X-User-Roles");   // Expecting "ROLE_ADMIN" or "ROLE_APPLICANT"
        String email = request.getHeader("X-User-Id");

        if (role != null && email != null) {
            // 2. Convert the role string into a Spring Authority
            // Note: If your Gateway sends "ADMIN", use "ROLE_" + role here.
            List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

            // 3. Create the internal Authentication object
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

            // 4. Set it in the Security Context for this specific request
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}