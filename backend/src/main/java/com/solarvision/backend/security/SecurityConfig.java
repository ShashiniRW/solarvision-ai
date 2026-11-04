package com.solarvision.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // every method that changes data
    private static final HttpMethod[] WRITE_METHODS = {
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> {
                    // the first matching rule wins, so specific rules come before general ones
                    auth.requestMatchers("/api/auth/register", "/api/auth/login").permitAll();
                    auth.requestMatchers("/error").permitAll();

                    // sensor readings: staff only, until devices get their own credentials
                    auth.requestMatchers(HttpMethod.POST, "/api/readings")
                            .hasAnyRole("ADMIN", "MANAGER", "ENGINEER", "TECHNICIAN");

                    // alerts and inspections: the fine-grained rules are @PreAuthorize on the controller methods
                    auth.requestMatchers("/api/alerts/**", "/api/inspections/**").authenticated();

                    // sites and equipment may be changed by administrators, managers and engineers
                    for (HttpMethod method : WRITE_METHODS) {
                        auth.requestMatchers(method,
                                        "/api/sites", "/api/sites/**",
                                        "/api/equipment", "/api/equipment/**")
                                .hasAnyRole("ADMIN", "MANAGER", "ENGINEER");
                    }

                    // secure by default: any other change under /api needs ADMIN or MANAGER
                    for (HttpMethod method : WRITE_METHODS) {
                        auth.requestMatchers(method, "/api/**").hasAnyRole("ADMIN", "MANAGER");
                    }

                    // everything else (reads) just needs a logged-in user
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}