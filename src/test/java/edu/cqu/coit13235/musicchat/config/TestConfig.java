package edu.cqu.coit13235.musicchat.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

/**
 * Test configuration that provides test-specific beans and disables certain components.
 * This configuration is only active during testing.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Provides a no-op password encoder for tests to avoid BCrypt complexity.
     * This makes tests faster and more predictable.
     * 
     * @return NoOpPasswordEncoder instance
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
