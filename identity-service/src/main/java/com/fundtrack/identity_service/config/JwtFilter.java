package com.fundtrack.identity_service.config;

import com.fundtrack.identity_service.exception.TokenExpiredException;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

/**
 * JwtFilter is a Spring Security filter responsible for validating incoming JWT tokens.
 * <p>
 * This filter:
 * <ul>
 * <li>Extracts the Authorization header</li>
 * <li>Parses and validates the JWT token</li>
 * <li>Retrieves user details and role from the token</li>
 * <li>Sets the authentication in the SecurityContext</li>
 * </ul>
 * It executes once per request as it extends {@link OncePerRequestFilter}.
 */
@Component
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver resolver;

    /**
     * Constructor for JwtFilter.
     * * @param jwtUtil The utility class for JWT operations.
     * @param resolver The resolver to bridge exceptions to the GlobalExceptionHandler.
     */
    public JwtFilter(
            JwtUtil jwtUtil,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtUtil = jwtUtil;
        this.resolver = resolver;
    }

    /**
     * Core filter logic that intercepts every request to check for a valid Bearer token.
     *
     * @param req The incoming HttpServletRequest.
     * @param res The outgoing HttpServletResponse.
     * @param chain The filter chain to proceed with if validation passes.
     * @throws ServletException If a servlet error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var claims = jwtUtil.parse(token);
                var roleClaim = String.valueOf(claims.get("role"));

                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + roleClaim))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("JWT authenticated request for {} with role={}", req.getRequestURI(), roleClaim);

            } catch (ExpiredJwtException e) {
                log.warn("JWT expired: {}", e.getMessage());
                // Delegate to GlobalExceptionHandler via the resolver
                resolver.resolveException(req, res, null, new TokenExpiredException("JWT token has expired. Please login again."));
                return; // Stop the filter chain here
            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                resolver.resolveException(req, res, null, e);
                return;
            }
        }

        chain.doFilter(req, res);
    }
}