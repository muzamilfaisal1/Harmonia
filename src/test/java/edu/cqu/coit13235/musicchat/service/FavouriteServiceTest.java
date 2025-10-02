package edu.cqu.coit13235.musicchat.service;

import edu.cqu.coit13235.musicchat.domain.Favourite;
import edu.cqu.coit13235.musicchat.repository.FavouriteRepository;
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
 * Unit tests for FavouriteService.
 */
@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {
    
    @Mock
    private FavouriteRepository favouriteRepository;
    
    @InjectMocks
    private FavouriteService favouriteService;
    
    private Favourite testFavourite;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_TRACK_ID = 1L;
    
    @BeforeEach
    void setUp() {
        testFavourite = new Favourite(TEST_USER_ID, TEST_TRACK_ID);
        testFavourite.setId(1L);
        testFavourite.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void toggleFavourite_NotFavourited_ShouldAddToFavourites() {
        // Given
        when(favouriteRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.empty());
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(testFavourite);
        
        // When
        boolean result = favouriteService.toggleFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertTrue(result);
        
        verify(favouriteRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(favouriteRepository).save(any(Favourite.class));
    }
    
    @Test
    void toggleFavourite_AlreadyFavourited_ShouldRemoveFromFavourites() {
        // Given
        when(favouriteRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.of(testFavourite));
        
        // When
        boolean result = favouriteService.toggleFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertFalse(result);
        
        verify(favouriteRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(favouriteRepository).delete(testFavourite);
    }
    
    @Test
    void isFavourited_ExistingFavourite_ShouldReturnTrue() {
        // Given
        when(favouriteRepository.existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(true);
        
        // When
        boolean result = favouriteService.isFavourited(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertTrue(result);
        
        verify(favouriteRepository).existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
    }
    
    @Test
    void isFavourited_NonExistentFavourite_ShouldReturnFalse() {
        // Given
        when(favouriteRepository.existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(false);
        
        // When
        boolean result = favouriteService.isFavourited(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertFalse(result);
        
        verify(favouriteRepository).existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
    }
    
    @Test
    void getUserFavourite_ExistingFavourite_ShouldReturnFavourite() {
        // Given
        when(favouriteRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.of(testFavourite));
        
        // When
        Optional<Favourite> result = favouriteService.getUserFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testFavourite, result.get());
        
        verify(favouriteRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
    }
    
    @Test
    void getUserFavourite_NonExistentFavourite_ShouldReturnEmpty() {
        // Given
        when(favouriteRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.empty());
        
        // When
        Optional<Favourite> result = favouriteService.getUserFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertFalse(result.isPresent());
        
        verify(favouriteRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
    }
    
    @Test
    void getTrackFavourites_ShouldReturnAllFavouritesForTrack() {
        // Given
        List<Favourite> favourites = List.of(testFavourite);
        when(favouriteRepository.findByTrackId(TEST_TRACK_ID)).thenReturn(favourites);
        
        // When
        List<Favourite> result = favouriteService.getTrackFavourites(TEST_TRACK_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testFavourite, result.get(0));
        
        verify(favouriteRepository).findByTrackId(TEST_TRACK_ID);
    }
    
    @Test
    void getUserFavourites_ShouldReturnAllFavouritesByUser() {
        // Given
        List<Favourite> favourites = List.of(testFavourite);
        when(favouriteRepository.findByUserId(TEST_USER_ID)).thenReturn(favourites);
        
        // When
        List<Favourite> result = favouriteService.getUserFavourites(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testFavourite, result.get(0));
        
        verify(favouriteRepository).findByUserId(TEST_USER_ID);
    }
    
    @Test
    void getFavouriteCount_ShouldReturnCount() {
        // Given
        when(favouriteRepository.countByTrackId(TEST_TRACK_ID)).thenReturn(3L);
        
        // When
        Long result = favouriteService.getFavouriteCount(TEST_TRACK_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(3L, result);
        
        verify(favouriteRepository).countByTrackId(TEST_TRACK_ID);
    }
    
    @Test
    void addFavourite_NotAlreadyFavourited_ShouldCreateFavourite() {
        // Given
        when(favouriteRepository.existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(false);
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(testFavourite);
        
        // When
        Favourite result = favouriteService.addFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(testFavourite, result);
        
        verify(favouriteRepository).existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(favouriteRepository).save(any(Favourite.class));
    }
    
    @Test
    void addFavourite_AlreadyFavourited_ShouldThrowException() {
        // Given
        when(favouriteRepository.existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(true);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            favouriteService.addFavourite(TEST_USER_ID, TEST_TRACK_ID);
        });
        
        verify(favouriteRepository).existsByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(favouriteRepository, never()).save(any(Favourite.class));
    }
    
    @Test
    void removeFavourite_ExistingFavourite_ShouldDeleteAndReturnTrue() {
        // Given
        when(favouriteRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.of(testFavourite));
        
        // When
        boolean result = favouriteService.removeFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertTrue(result);
        
        verify(favouriteRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(favouriteRepository).delete(testFavourite);
    }
    
    @Test
    void removeFavourite_NonExistentFavourite_ShouldReturnFalse() {
        // Given
        when(favouriteRepository.findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID))
            .thenReturn(Optional.empty());
        
        // When
        boolean result = favouriteService.removeFavourite(TEST_USER_ID, TEST_TRACK_ID);
        
        // Then
        assertFalse(result);
        
        verify(favouriteRepository).findByUserIdAndTrackId(TEST_USER_ID, TEST_TRACK_ID);
        verify(favouriteRepository, never()).delete(any(Favourite.class));
    }
}
