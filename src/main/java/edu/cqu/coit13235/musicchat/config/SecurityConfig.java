package edu.cqu.coit13235.musicchat.config;

import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for MusicChat application.
 * Provides session-based authentication with form login and logout.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Password encoder bean for hashing passwords.
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * User details service that loads users from the database.
     * 
     * @return UserDetailsService implementation
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
                
                return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole())
                    .build();
            }
        };
    }
    
    /**
     * Security filter chain configuration.
     * Secures all endpoints and configures form-based authentication.
     * 
     * @param http HttpSecurity configuration
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for development
            .authorizeHttpRequests(authz -> authz
                // Public web pages
                .requestMatchers("/", "/signup", "/signin", "/h2-console/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/tests").permitAll() // Public test results endpoint
                
                // Public GET API endpoints (read-only)
                .requestMatchers("GET", "/api/audio/tracks", "/api/audio/tracks/*", "/api/audio/count").permitAll()
                .requestMatchers("GET", "/api/audio/search/**", "/api/audio/download/*").permitAll()
                .requestMatchers("GET", "/api/audio/*/ratings", "/api/audio/*/favourites").permitAll()
                .requestMatchers("GET", "/api/audio/favorites").permitAll()
                .requestMatchers("GET", "/api/chat/messages", "/api/chat/messages/*", "/api/chat/messages/sender/*", "/api/chat/messages/count").permitAll()
                .requestMatchers("GET", "/api/playlists", "/api/playlists/*", "/api/playlists/*/tracks").permitAll()
                .requestMatchers("GET", "/api/playlists/owner/*", "/api/playlists/search", "/api/playlists/count/**").permitAll()
                .requestMatchers("GET", "/api/external/**").permitAll()
                .requestMatchers("GET", "/tests").permitAll()
                .requestMatchers("/ws-chat/**").permitAll() // WebSocket endpoint
                
                // Public POST endpoints (rating/favourite use userId in request body, playlist creation)
                .requestMatchers("POST", "/api/audio/*/rate", "/api/audio/*/favorite").permitAll()
                .requestMatchers("POST", "/api/playlists", "/api/playlists/*/tracks").permitAll()
                .requestMatchers("PUT", "/api/playlists/*").permitAll()
                .requestMatchers("DELETE", "/api/playlists/*", "/api/playlists/*/tracks").permitAll()
                
                // Protected API endpoints (require authentication)
                .requestMatchers("POST", "/api/chat/messages").authenticated() // Chat requires auth
                .requestMatchers("POST", "/api/audio/upload").authenticated() // Upload requires auth
                .requestMatchers("GET", "/api/audio/tracks/my").authenticated() // My tracks requires auth
                .requestMatchers("DELETE", "/api/audio/tracks/*").authenticated() // Delete requires auth
                .requestMatchers("/api/me").authenticated()
                
                // Web pages requiring authentication
                .requestMatchers("/chat", "/audio", "/dashboard").authenticated()
                
                // All other requests require authentication by default
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/signin")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/signin?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .httpBasic(httpBasic -> {}) // Enable HTTP Basic for API testing
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // For API requests, return 401 instead of redirecting
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(401, "Unauthorized");
                    } else {
                        response.sendRedirect("/signin");
                    }
                })
            )
            .userDetailsService(userDetailsService())
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Allow H2 console frames
            );
        
        return http.build();
    }
}
