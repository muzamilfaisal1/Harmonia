package edu.cqu.coit13235.musicchat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import edu.cqu.coit13235.musicchat.domain.Playlist;

/**
 * Repository interface for Playlist entity.
 * Provides data access methods for playlists.
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    
    /**
     * Find playlists by owner ID.
     * @param ownerId The owner's ID
     * @return List of playlists owned by the specified user
     */
    List<Playlist> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    
    /**
     * Find playlist by ID and owner ID (for security).
     * @param id The playlist ID
     * @param ownerId The owner's ID
     * @return Optional containing the playlist if found and owned by the user
     */
    Optional<Playlist> findByIdAndOwnerId(Long id, Long ownerId);
    
    /**
     * Find playlists by name containing the given text (case-insensitive).
     * @param name The name text to search for
     * @return List of playlists with names containing the text
     */
    @Query("SELECT p FROM Playlist p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.createdAt DESC")
    List<Playlist> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find all playlists ordered by creation date (newest first).
     * @return List of all playlists ordered by creation date
     */
    @Query("SELECT p FROM Playlist p ORDER BY p.createdAt DESC")
    List<Playlist> findAllOrderByCreatedAtDesc();
    
    /**
     * Count total number of playlists.
     * @return Total count of playlists
     */
    @Override
    long count();
    
    /**
     * Count playlists by owner ID.
     * @param ownerId The owner's ID
     * @return Number of playlists owned by the specified user
     */
    long countByOwnerId(Long ownerId);
}
