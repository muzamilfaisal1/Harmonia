package edu.cqu.coit13235.musicchat.repository;

import edu.cqu.coit13235.musicchat.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides methods for user data access and authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find a user by username.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if a user exists with the given username.
     * 
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if a user exists with the given email.
     * 
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}
