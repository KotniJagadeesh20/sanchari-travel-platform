package com.travelplatform.packages.config;

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
     * Matches GET /packages/{uuid} and GET /destinations/{uuid} specifically
     * — NOT a plain "/packages/*" Ant matcher, which would also match GET
     * /packages/bookings (a customer's own bookings, must stay
     * authenticated). Both are the same single-segment path shape, so only
     * the UUID format tells them apart — same approach as ride-share-service
     * distinguishing /rides/{uuid} from /rides/driver and /rides/bookings.
     * Keep these two regexes in sync with JwtAuthFilter in api-gateway.
     */
    private static final RegexRequestMatcher PACKAGE_DETAILS_GET = new RegexRequestMatcher(
            "^/packages/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            HttpMethod.GET.name());
    private static final RegexRequestMatcher DESTINATION_DETAILS_GET = new RegexRequestMatcher(
            "^/destinations/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            HttpMethod.GET.name());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Admin subtrees must be matched before the general /packages/** and
                // /destinations/** rules below, or those broader rules would shadow them.
                .requestMatchers("/packages/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/destinations/admin/**").hasAuthority("ROLE_ADMIN")
                // Browsing is public — search/list/details — matching Bus, Hotel, and
                // Ride. Only booking, creating, and admin management need an account.
                .requestMatchers(new AntPathRequestMatcher("/packages", HttpMethod.GET.name())).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/packages/by-destination/**", HttpMethod.GET.name())).permitAll()
                .requestMatchers(PACKAGE_DETAILS_GET).permitAll()
                .requestMatchers("/packages/**").authenticated()
                .requestMatchers(new AntPathRequestMatcher("/destinations", HttpMethod.GET.name())).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/destinations/search", HttpMethod.GET.name())).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/destinations/popular", HttpMethod.GET.name())).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/destinations/category/**", HttpMethod.GET.name())).permitAll()
                .requestMatchers(DESTINATION_DETAILS_GET).permitAll()
                .requestMatchers("/destinations/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(new JwtValidator(), BasicAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .httpBasic(h -> h.disable())
            .formLogin(f -> f.disable());
        return http.build();
    }
}
