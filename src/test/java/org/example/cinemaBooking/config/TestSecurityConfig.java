package org.example.cinemaBooking.config;

import org.example.cinemaBooking.Config.CustomJwtDecoder;
import org.example.cinemaBooking.Config.JwtAuthenticationEntryPoint;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class TestSecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/introspect",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            // Payment gateway webhook (IPN) called by VNPay
            "/api/v1/payments/vnpay/ipn",
            // Promotion preview/apply endpoints (kept public if frontend needs them before auth)
            "/api/v1/promotions/preview",
            "/api/v1/promotions/apply",
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            // Auth
            "/api/v1/auth/reset-password/validate",
            // Movies - public browsing
            "/api/v1/movies",
            "/api/v1/movies/*/images",
            "/api/v1/movies/slug/*",
            "/api/v1/movies/now-showing",
            "/api/v1/movies/coming-soon",
            "/api/v1/movies/search/**",
            "/api/v1/movies/*",
            // Showtimes
            "/api/v1/showtimes/*",
            "/api/v1/showtimes/by-movie/**",
            "/api/v1/showtimes/by-cinema/**",
            // Seat map (public) - exclude the "my-locked-seats" which requires auth
            "/api/v1/showtimes/*/seats",
            // Promotions
            "/api/v1/promotions/active",
            "/api/v1/promotions/*",
            "/api/v1/promotions/code/*",
            // Products
            "/api/v1/products",
            "/api/v1/products/active",
            "/api/v1/products/*",
            // Cinemas
            "/api/v1/cinema",
            "/api/v1/cinema/*",
            "/api/v1/cinema/*/movies",
            "/api/v1/cinema/*/rooms",
            // Categories
            "/api/v1/categories",
            "/api/v1/categories/*",
            // Rooms
            "/api/v1/rooms",
            "/api/v1/rooms/*",
            "/api/v1/rooms/cinema/*",
            // Reviews (read-only)
            "/api/v1/reviews/*",
            "/api/v1/reviews/movies/*",
            "/api/v1/reviews/movies/*/average-rating",
            // Payment return (gateway redirect)
            "/api/v1/payments/vnpay/return",
            "/api/v1/payments/vnpay/ipn",
            //Seat
            "/api/v1/seats/*",
            "/api/v1/seat-types",
            "/api/v1/seat-types/*",
            "/api/v1/seats/rooms/*",
            //combo
            "/api/v1/combos/active",
            "/api/v1/combos/*",

    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)

                // JWT là stateless — không dùng session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}