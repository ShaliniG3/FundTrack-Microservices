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

@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract values injected by your API Gateway
        String role = request.getHeader("X-User-Role");   // Expecting "ROLE_ADMIN" or "ROLE_APPLICANT"
        String email = request.getHeader("X-User-Email");

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