package com.example.backend.Security;

import com.example.backend.repository.UtilisateurRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable @PreAuthorize and @PostAuthorize annotations
public class SecurityConfig {
    
    private final UtilisateurRepository utilisateurRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Constructor injection instead of field injection
    public SecurityConfig(UtilisateurRepository utilisateurRepository, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.utilisateurRepository = utilisateurRepository;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/users/login", "/users/register", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    
                    // User management
                    .requestMatchers(HttpMethod.GET, "/users").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    .requestMatchers("/users/client-admin/**").hasRole("SUPER_ADMIN")
                    .requestMatchers("/users/agent-inventaire/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    // Client management
                    .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                    .requestMatchers(HttpMethod.POST, "/api/clients/**").hasRole("SUPER_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("SUPER_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("SUPER_ADMIN")
                        //Categ
                    .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/produits/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                    .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/produits/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/produits/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/produits/**").hasRole("SUPER_ADMIN")
                        //plans
                    .requestMatchers(HttpMethod.GET, "Plans/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                    .requestMatchers(HttpMethod.POST, "Plans/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    .requestMatchers(HttpMethod.PUT, "Plans/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    // Zone manage
                    .requestMatchers(HttpMethod.GET, "/api/zones/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                    .requestMatchers(HttpMethod.POST, "/api/zones/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    .requestMatchers(HttpMethod.PUT, "/api/zones/**").hasAnyRole("SUPER_ADMIN", "ADMIN_CLIENT")
                    .requestMatchers(HttpMethod.DELETE, "/api/zones/**").hasRole("SUPER_ADMIN")
                    //lkhrin authaut
                    .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> utilisateurRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}