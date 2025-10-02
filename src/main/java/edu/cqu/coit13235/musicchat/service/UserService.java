package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Service class for user management and authentication.
 * Handles user registration, password encryption, and Spring Security integration.
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Save a new user with encrypted password.
     * 
     * @param user the user to save
     * @return the saved user
     */
    public User save(User user) {
        // Encrypt password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    
    /**
     * Check if username already exists.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Check if email already exists.
     * 
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
