package com.cts.fundtrack.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

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