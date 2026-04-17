// package com.cts.fundtrack.gateway.config;

// import java.util.List;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.config.web.server.ServerHttpSecurity;
// import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
// import org.springframework.security.core.userdetails.User;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import org.springframework.security.web.server.SecurityWebFilterChain;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.CorsConfigurationSource;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


// @Configuration
// @EnableWebFluxSecurity
// public class SecurityConfig {

//     @Bean
//     public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//         http
//             .csrf(ServerHttpSecurity.CsrfSpec::disable)
//             .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//             .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//             .authorizeExchange(exchanges -> exchanges
//                 .anyExchange().permitAll()
//             );
//         return http.build();
//     }
// }
//     /**
//      * This Bean is the "Secret Sauce." 
//      * Providing this stops Spring from generating that random console password.
//      */
//     // @Bean
//     // public MapReactiveUserDetailsService userDetailsService() {
//     //     UserDetails user = User.withDefaultPasswordEncoder()
//     //         .username("gateway-admin")
//     //         .password("password")
//     //         .roles("ADMIN")
//     //         .build();
//     //     return new MapReactiveUserDetailsService(user);
//     // }



package com.cts.fundtrack.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            );
        return http.build();
    }
}