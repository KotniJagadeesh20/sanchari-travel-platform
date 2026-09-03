package com.travelplatform.rideshare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Matches GET /rides/{uuid} specifically — NOT a plain "/rides/*" Ant
     * matcher, which would also match GET /rides/driver and GET
     * /rides/bookings (a driver's own rides / a passenger's own bookings,
     * both of which must stay authenticated). Those are the same path shape
     * as /rides/{rideId}, so only the UUID format tells them apart. Keep
     * this in sync with JwtAuthFilter's RIDE_ID_PATTERN in api-gateway.
     */
    private static final RegexRequestMatcher RIDE_DETAILS_GET = new RegexRequestMatcher(
            "^/rides/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            HttpMethod.GET.name());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(new AntPathRequestMatcher("/rides/search", HttpMethod.GET.name())).permitAll()
                .requestMatchers(RIDE_DETAILS_GET).permitAll()
                .requestMatchers("/rides/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(new JwtValidator(), BasicAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .httpBasic(h -> h.disable())
            .formLogin(f -> f.disable());
        return http.build();
    }
}
