package com.cts.fundtrack.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Shared Feign client configuration that propagates gateway-injected security headers
 * on every outbound inter-service HTTP call.
 *
 * <p>When the API Gateway authenticates a request it stamps three custom headers onto
 * the forwarded request:</p>
 * <ul>
 *   <li>{@code X-User-Id} — UUID of the authenticated user</li>
 *   <li>{@code X-User-Role} — primary role of the authenticated user</li>
 *   <li>{@code X-User-Email} — email address of the authenticated user</li>
 * </ul>
 *
 * <p>The {@link RequestInterceptor} defined here reads those headers from the current
 * {@link jakarta.servlet.http.HttpServletRequest} and copies them onto every Feign
 * request template, ensuring downstream services receive the same identity context
 * without each caller needing to pass headers manually.</p>
 *
 * <p>This class is component-scanned by all microservices that declare
 * {@code scanBasePackages = "com.cts.fundtrack"} in their main application class.</p>
 */
@Configuration
public class FeignConfig {

    /**
     * Creates a Feign {@link RequestInterceptor} that copies the three gateway security
     * headers ({@code X-User-Id}, {@code X-User-Role}, {@code X-User-Email}) from the
     * current servlet request onto every outgoing Feign call.
     *
     * <p>If there is no active servlet request context (e.g., during async processing),
     * the interceptor is a no-op and no headers are added.</p>
     *
     * @return a configured {@link RequestInterceptor} for header propagation
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Pull the headers from the Gateway-to-Application call
                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                String userEmail = request.getHeader("X-User-Email");

                // "Stamp" them onto the Application-to-Program call
                if (userId != null) requestTemplate.header("X-User-Id", userId);
                if (userRole != null) requestTemplate.header("X-User-Role", userRole);
                if (userEmail != null) requestTemplate.header("X-User-Email", userEmail);
            }
        };
    }
}