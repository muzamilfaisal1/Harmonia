package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.Rating;
import edu.cqu.coit13235.musicchat.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RatingService.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {
    
    @Mock
    private RatingRepository ratingRepository;
    
    @InjectMocks
    private RatingService ratingService;
    
    private Rating testRating;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_TRACK_ID = 1L;
    private final Integer TEST_RATING_VALUE = 4;
    
    @BeforeEach
    void setUp() {
        testRating = new Rating(TEST_USER_ID, TEST_TRACK_ID, TEST_RATING_VALUE);
        testRating.setId(1L);
        testRating.setCreatedAt(LocalDateTime.now());
        testRating.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    void rateTrack_NewRating_ShouldCreateNewRating() {
        // Given
        when(ratingRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        
        // When
        Rating result = ratingService.rateTrack(TEST_USER_ID, TEST_TRACK_ID, TEST_RATING_VALUE);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_TRACK_ID, result.getTrackId());
        assertEquals(TEST_RATING_VALUE, result.getRatingValue());
        
        verify(ratingRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(ratingRepository).save(any(Rating.class));
    }
    
    @Test
    void rateTrack_ExistingRating_ShouldUpdateRating() {
        // Given
        Rating existingRating = new Rating(TEST_USER_ID, TEST_TRACK_ID, 3);
        existingRating.setId(1L);
        existingRating.setCreatedAt(LocalDateTime.now().minusHours(1));
        
        when(ratingRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.of(existingRating));
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        
        // When
        Rating result = ratingService.rateTrack(TEST_USER_ID, TEST_TRACK_ID, TEST_RATING_VALUE);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_RATING_VALUE, result.getRatingValue());
        assertNotNull(result.getUpdatedAt());
        
        verify(ratingRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(ratingRepository).save(existingRating);
    }
    
    @Test
    void rateTrack_InvalidRatingValue_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.rateTrack(TEST_USER_ID, TEST_TRACK_ID, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.rateTrack(TEST_USER_ID, TEST_TRACK_ID, 6);
        });
        
        verify(ratingRepository, never()).save(any(Rating.class));
    }
    
    @Test
    void getUserRating_ExistingRating_ShouldReturnRating() {
        // Given
        when(ratingRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.of(testRating));
        
        // When
        Optional<Rating> result = ratingService.getUserRating(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testRating, result.get());
        
        verify(ratingRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
    }
    
    @Test
    void getUserRating_NonExistentRating_ShouldReturnEmpty() {
        // Given
        when(ratingRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.empty());
        
        // When
        Optional<Rating> result = ratingService.getUserRating(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertFalse(result.isPresent());
        
        verify(ratingRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
    }
    
    @Test
    void getTrackRatings_ShouldReturnAllRatingsForTrack() {
        // Given
        List<Rating> ratings = List.of(testRating);
        when(ratingRepository.findByTrackId(TEST_TRACK_ID)).thenReturn(ratings);
        
        // When
        List<Rating> result = ratingService.getTrackRatings(TEST_TRACK_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRating, result.get(0));
        
        verify(ratingRepository).findByTrackId(TEST_TRACK_ID);
    }
    
    @Test
    void getUserRatings_ShouldReturnAllRatingsByUser() {
        // Given
        List<Rating> ratings = List.of(testRating);
        when(ratingRepository.findByUserId(TEST_USER_ID)).thenReturn(ratings);
        
        // When
        List<Rating> result = ratingService.getUserRatings(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRating, result.get(0));
        
        verify(ratingRepository).findByUserId(TEST_USER_ID);
    }
    
    @Test
    void getAverageRating_WithRatings_ShouldReturnAverage() {
        // Given
        when(ratingRepository.findAverageRatingByTrackId(TEST_TRACK_ID)).thenReturn(4.5);
        
        // When
        Double result = ratingService.getAverageRating(TEST_TRACK_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(4.5, result, 0.01);
        
        verify(ratingRepository).findAverageRatingByTrackId(TEST_TRACK_ID);
    }
    
    @Test
    void getAverageRating_NoRatings_ShouldReturnNull() {
        // Given
        when(ratingRepository.findAverageRatingByTrackId(TEST_TRACK_ID)).thenReturn(null);
        
        // When
        Double result = ratingService.getAverageRating(TEST_TRACK_ID);
        
        // Then
        assertNull(result);
        
        verify(ratingRepository).findAverageRatingByTrackId(TEST_TRACK_ID);
    }
    
    @Test
    void getRatingCount_ShouldReturnCount() {
        // Given
        when(ratingRepository.countByTrackId(TEST_TRACK_ID)).thenReturn(5L);
        
        // When
        Long result = ratingService.getRatingCount(TEST_TRACK_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(5L, result);
        
        verify(ratingRepository).countByTrackId(TEST_TRACK_ID);
    }
    
    @Test
    void removeRating_ExistingRating_ShouldDeleteAndReturnTrue() {
        // Given
        when(ratingRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.of(testRating));
        
        // When
        boolean result = ratingService.removeRating(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertTrue(result);
        
        verify(ratingRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(ratingRepository).delete(testRating);
    }
    
    @Test
    void removeRating_NonExistentRating_ShouldReturnFalse() {
        // Given
        when(ratingRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.empty());
        
        // When
        boolean result = ratingService.removeRating(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertFalse(result);
        
        verify(ratingRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(ratingRepository, never()).delete(any(Rating.class));
    }
}
