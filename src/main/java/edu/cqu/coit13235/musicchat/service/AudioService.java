package edu.cqu.coit13235.musicchat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.cqu.coit13235.musicchat.domain.AudioTrack;
import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.repository.AudioTrackRepository;
import edu.cqu.coit13235.musicchat.repository.UserRepository;

/**
 * Service class for audio file handling and storage.
 * Handles file upload, storage, and metadata management.
 */
@Service
@Transactional
public class AudioService {
    
    private final AudioTrackRepository audioTrackRepository;
    private final UserRepository userRepository;
    private final String uploadDir;
    private final long maxFileSize;
    
    @Autowired
    public AudioService(AudioTrackRepository audioTrackRepository,
                       UserRepository userRepository,
                       @Value("${app.upload.dir:uploads}") String uploadDir,
                       @Value("${app.upload.max-file-size:52428800}") long maxFileSize) {
        this.audioTrackRepository = audioTrackRepository;
        this.userRepository = userRepository;
        this.uploadDir = uploadDir;
        this.maxFileSize = maxFileSize;
        
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }
    
    /**
     * Upload and save an audio file.
     * @param file The multipart file to upload
     * @param title The title of the audio track
     * @param artist The artist name
     * @return The saved AudioTrack entity
     * @throws IllegalArgumentException if file is invalid
     * @throws IOException if file operations fail
     */
    public AudioTrack uploadAudio(MultipartFile file, String title, String artist) throws IOException {
        validateFile(file);
        validateTitleAndArtist(title, artist);
        
        // Get the currently authenticated user
        User currentUser = getCurrentUser();
        
        return uploadAudio(file, title, artist, currentUser);
    }
    
    /**
     * Upload and save an audio file with a specific user.
     * This method is primarily for testing purposes.
     * @param file The multipart file to upload
     * @param title The title of the audio track
     * @param artist The artist name
     * @param user The user uploading the file
     * @return The saved AudioTrack entity
     * @throws IllegalArgumentException if file is invalid
     * @throws IOException if file operations fail
     */
    public AudioTrack uploadAudio(MultipartFile file, String title, String artist, User user) throws IOException {
        validateFile(file);
        validateTitleAndArtist(title, artist);
        
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Save file to filesystem
        Path targetPath = Paths.get(uploadDir, uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create and save AudioTrack entity
        AudioTrack audioTrack = new AudioTrack(
            title,
            artist,
            uniqueFilename,
            originalFilename,
            null, // Duration will be calculated later if needed
            file.getSize(),
            file.getContentType(),
            user
        );
        
        return audioTrackRepository.save(audioTrack);
    }
    
    /**
     * Get all audio tracks ordered by upload date.
     * @return List of all audio tracks
     */
    @Transactional(readOnly = true)
    public List<AudioTrack> getAllTracks() {
        return audioTrackRepository.findAllOrderByUploadedAtDesc();
    }
    
    /**
     * Get audio track by ID.
     * @param id The track ID
     * @return Optional containing the track if found
     */
    @Transactional(readOnly = true)
    public Optional<AudioTrack> getTrackById(Long id) {
        return audioTrackRepository.findById(id);
    }
    
    /**
     * Get audio track by filename.
     * @param filename The filename
     * @return Optional containing the track if found
     */
    @Transactional(readOnly = true)
    public Optional<AudioTrack> getTrackByFilename(String filename) {
        return audioTrackRepository.findByFilename(filename);
    }
    
    /**
     * Search tracks by title.
     * @param title The title to search for
     * @return List of tracks matching the title
     */
    @Transactional(readOnly = true)
    public List<AudioTrack> searchTracksByTitle(String title) {
        return audioTrackRepository.findByTitleContainingIgnoreCase(title);
    }
    
    /**
     * Search tracks by artist.
     * @param artist The artist to search for
     * @return List of tracks by the artist
     */
    @Transactional(readOnly = true)
    public List<AudioTrack> searchTracksByArtist(String artist) {
        return audioTrackRepository.findByArtistContainingIgnoreCase(artist);
    }
    
    /**
     * Delete an audio track and its file.
     * @param id The track ID
     * @return true if deleted successfully, false if not found
     */
    public boolean deleteTrack(Long id) {
        Optional<AudioTrack> trackOpt = audioTrackRepository.findById(id);
        if (trackOpt.isPresent()) {
            AudioTrack track = trackOpt.get();
            
            // Delete file from filesystem
            try {
                Path filePath = Paths.get(uploadDir, track.getFilename());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but continue with database deletion
                System.err.println("Failed to delete file: " + e.getMessage());
            }
            
            // Delete from database
            audioTrackRepository.delete(track);
            return true;
        }
        return false;
    }
    
    /**
     * Get the file path for an audio track.
     * @param track The audio track
     * @return Path to the audio file
     */
    public Path getFilePath(AudioTrack track) {
        return Paths.get(uploadDir, track.getFilename());
    }
    
    /**
     * Check if a file exists for the given track.
     * @param track The audio track
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(AudioTrack track) {
        return Files.exists(getFilePath(track));
    }
    
    /**
     * Get total number of tracks.
     * @return Total count of tracks
     */
    @Transactional(readOnly = true)
    public long getTrackCount() {
        return audioTrackRepository.count();
    }
    
    /**
     * Get audio tracks uploaded by a specific user.
     * @param user The user who uploaded the tracks
     * @return List of tracks uploaded by the user
     */
    @Transactional(readOnly = true)
    public List<AudioTrack> getTracksByUser(User user) {
        return audioTrackRepository.findByUserOrderByUploadedAtDesc(user);
    }
    
    /**
     * Get audio tracks uploaded by the current authenticated user.
     * @return List of tracks uploaded by the current user
     */
    @Transactional(readOnly = true)
    public List<AudioTrack> getTracksByCurrentUser() {
        User currentUser = getCurrentUser();
        return audioTrackRepository.findByUserOrderByUploadedAtDesc(currentUser);
    }
    
    /**
     * Get the currently authenticated user.
     * @return The current user
     * @throws IllegalStateException if no user is authenticated
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
    
    /**
     * Validate uploaded file.
     * @param file The multipart file to validate
     * @throws IllegalArgumentException if file is invalid
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isValidAudioType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only audio files are allowed");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be null or empty");
        }
    }
    
    private void validateTitleAndArtist(String title, String artist) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        
        if (artist == null || artist.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist cannot be null or empty");
        }
    }
    
    /**
     * Check if the content type is a valid audio type.
     * @param contentType The content type to check
     * @return true if valid audio type, false otherwise
     */
    private boolean isValidAudioType(String contentType) {
        return contentType.startsWith("audio/") || 
               contentType.equals("application/octet-stream") ||
               contentType.equals("video/mp4"); // Some audio files might be detected as video
    }
    
    /**
     * Get file extension from filename.
     * @param filename The filename
     * @return The file extension including the dot
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
