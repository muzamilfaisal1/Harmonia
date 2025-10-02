package edu.cqu.coit13235.musicchat.repository;

import edu.cqu.coit13235.musicchat.domain.Favourite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Favourite entity operations.
 * Provides methods for favourite data access and management.
 */
@Repository
public interface FavouriteRepository extends JpaRepository<Favourite, Long> {
    
    /**
     * Find a favourite by user ID and track ID.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return Optional containing the favourite if found, empty otherwise
     */
    Optional<Favourite> findByUserIdAndTrackId(Long userId, Long trackId);
    
    /**
     * Find all favourites for a specific track.
     * 
     * @param trackId the track ID
     * @return List of favourites for the track
     */
    List<Favourite> findByTrackId(Long trackId);
    
    /**
     * Find all favourites by a specific user.
     * 
     * @param userId the user ID
     * @return List of favourites by the user
     */
    List<Favourite> findByUserId(Long userId);
    
    /**
     * Check if a track is favourited by a user.
     * 
     * @param userId the user ID
     * @param trackId the track ID
     * @return true if favourited, false otherwise
     */
    boolean existsByUserIdAndTrackId(Long userId, Long trackId);
    
    /**
     * Count the number of favourites for a track.
     * 
     * @param trackId the track ID
     * @return number of favourites
     */
    Long countByTrackId(Long trackId);
}
