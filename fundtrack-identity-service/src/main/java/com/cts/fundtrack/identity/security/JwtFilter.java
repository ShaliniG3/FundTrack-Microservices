package com.cts.fundtrack.identity.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.cts.fundtrack.common.exceptions.TokenExpiredException;

/**
 * Servlet filter that validates JWT Bearer tokens on every incoming HTTP request.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee exactly one execution per
 * request, regardless of how many filter chains are active in the application.</p>
 *
 * <p>Filter behaviour:</p>
 * <ol>
 *   <li>Public paths (auth endpoints, Swagger UI, OpenAPI docs) are bypassed via
 *       {@link #shouldNotFilter(HttpServletRequest)}.</li>
 *   <li>For all other paths, the {@code Authorization} header is inspected for a
 *       {@code Bearer} token.</li>
 *   <li>If a token is present it is parsed and validated with {@link JwtUtil}. On
 *       success, a {@link UsernamePasswordAuthenticationToken} is placed into the
 *       {@link SecurityContextHolder} so that downstream security checks can
 *       identify the caller.</li>
 *   <li>If the token has expired, a {@link TokenExpiredException} is forwarded to
 *       the global exception handler via the injected {@link HandlerExceptionResolver},
 *       and the filter chain is halted.</li>
 *   <li>Any other JWT parsing failure is similarly delegated to the exception
 *       handler.</li>
 * </ol>
 *
 * <p>Requests that carry no {@code Authorization} header pass through the filter
 * unchanged and will be rejected later by Spring Security's access-control rules
 * if the endpoint requires authentication.</p>
 *
 * @see JwtUtil
 * @see com.cts.fundtrack.identity.config.SecurityConfig
 */
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver resolver;

    /**
     * Constructs the filter with the required JWT utility and exception resolver.
     *
     * <p>The {@link HandlerExceptionResolver} is injected with the
     * {@code "handlerExceptionResolver"} qualifier to target the composite resolver
     * that delegates to {@code @ExceptionHandler} methods, bridging servlet-level
     * filter exceptions into the MVC exception-handling pipeline.</p>
     *
     * @param jwtUtil  the utility component used to parse and validate JWT tokens
     * @param resolver the exception resolver used to forward token errors to the
     *                 global exception handler
     */
    public JwtFilter(
            JwtUtil jwtUtil,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtUtil = jwtUtil;
        this.resolver = resolver;
    }

    /**
     * Determines whether this filter should be skipped for the given request.
     *
     * <p>Skipped for:</p>
     * <ul>
     *   <li>{@code /api/v1/auth/**} — public authentication endpoints.</li>
     *   <li>{@code /v3/api-docs/**} — OpenAPI specification endpoints.</li>
     *   <li>{@code /swagger-ui/**} — Swagger UI static resources.</li>
     * </ul>
     *
     * @param request the current HTTP request
     * @return {@code true} if the filter should be bypassed for this request
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-ui");
    }

    /**
     * Core filter logic that validates the JWT Bearer token and populates the
     * {@link SecurityContextHolder} for the duration of the request.
     *
     * <p>If the {@code Authorization} header contains a valid {@code Bearer} token,
     * the subject (email) and {@code role} claim are extracted and used to build a
     * fully authenticated {@link UsernamePasswordAuthenticationToken} with the
     * appropriate {@link SimpleGrantedAuthority}.</p>
     *
     * <p>Exceptions are not thrown directly; they are delegated to the
     * {@link HandlerExceptionResolver} so that the configured
     * {@code @ExceptionHandler} methods can produce consistent JSON error responses.</p>
     *
     * @param req   the incoming HTTP request
     * @param res   the outgoing HTTP response
     * @param chain the filter chain to continue if validation succeeds
     * @throws ServletException if a servlet-level error occurs during filtering
     * @throws IOException      if an I/O error occurs while processing the request
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
