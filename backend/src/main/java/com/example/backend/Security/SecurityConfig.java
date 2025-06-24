package com.example.backend.Security;

import com.example.backend.repository.UtilisateurRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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
@EnableMethodSecurity
public class SecurityConfig {
    
    private final UtilisateurRepository utilisateurRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;


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
                        .requestMatchers("/checkups/ajouter","/checkups/scan","/produits","/api/plans","/api/plans/**","produits/byZone/*","Zone/all","api/plans/names-dates","produits/names-dates","produits","users/countAdminClient","api/plans/countByStatus","users/names-dates","api/plans/countterminer","produits/count","Zone/count","/users/login","users/countAgentInventaire", "/users/register", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        //produit management
                        .requestMatchers(HttpMethod.PUT,"/produits/{id}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.DELETE,"/produits/{produitId}").hasAnyAuthority("SUPER_ADMIN", "ROLE_SUPER_ADMIN", "ADMIN_CLIENT", "ROLE_ADMIN_CLIENT")
                        //checkup management

                        .requestMatchers(HttpMethod.PUT,"/checkups/{checkupId}/recomptage","/checkups/{checkupId}/valider").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.GET,"/checkups/plan/{id}","/checkups/plan/{planId}/logs","/checkups/plan/{planId}/type/{type}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                        // User management
                        .requestMatchers(HttpMethod.GET ,"/users","users/count","users/countAdminClient").hasAnyAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET ,"/users/agents","AgentInventaire","users/countAgentInventaire","users/names-dates").hasAnyAuthority("SUPER_ADMIN","ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.POST,"/users/client-admin/**").hasAnyAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST,"users/agent-inventaire/{clientId}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.DELETE,"/users/{userId}").hasAnyAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/users/{userId}").hasAnyAuthority("SUPER_ADMIN")
                    .requestMatchers(HttpMethod.POST,"AgentInventaire/assign/{planId}/{agentId}/{zoneId}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        // Client management
                        .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                        .requestMatchers(HttpMethod.POST, "/api/clients/**").hasAnyAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasAnyAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasAnyAuthority("SUPER_ADMIN")
                        //Categ
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/{id}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyAuthority("SUPER_ADMIN","ADMIN_CLIENT")
                        //plans
                        .requestMatchers(HttpMethod.GET,"api/plans","api/plans/{planId}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                        .requestMatchers(HttpMethod.GET, "/api/plans/{planId}/produits","/api/plans/{planId}/details","/api/plans/{planId}/zone-products","/api/plans/createdby/{userId}","/api/plans/createdby/me").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/plans","/api/plans/{planId}/zones","/api/plans/{planId}/produits","/api/plans/{planId}/agents/{agentId}/assignations").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.PUT, "api/plans/{planId}/statut","api/plans/{planId}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        // Zone manage
                        .requestMatchers(HttpMethod.GET, "/Zone/all","/Zones/{zoneId}/products","/Zone/count").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE")
                        .requestMatchers(HttpMethod.POST, "/Zone","/Zones/{zoneId}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.PUT, "/Zone/update/{id}").hasAnyAuthority("SUPER_ADMIN", "ADMIN_CLIENT")
                        .requestMatchers(HttpMethod.DELETE, "/Zones/{zoneId}","/Zones/{zoneId}/products/{productId}").hasAnyAuthority("SUPER_ADMIN","ADMIN_CLIENT")
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