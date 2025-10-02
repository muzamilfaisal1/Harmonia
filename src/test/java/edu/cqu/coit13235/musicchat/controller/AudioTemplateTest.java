package edu.cqu.coit13235.musicchat.controller;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests for the audio management template rendering.
 * Validates that the Thymeleaf templates are properly configured and render correctly.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AudioTemplateTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    
    @Test
    void testAudioPageRenders() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(view().name("audio"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("MusicChat - Audio Management")))
                .andExpect(content().string(containsString("Audio Management")))
                .andExpect(content().string(containsString("upload-form")))
                .andExpect(content().string(containsString("playlist-form")));
    }
    
    @Test
    void testAudioPageContainsUploadForm() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("upload-form")))
                .andExpect(content().string(containsString("name=\"title\"")))
                .andExpect(content().string(containsString("name=\"artist\"")))
                .andExpect(content().string(containsString("name=\"file\"")))
                .andExpect(content().string(containsString("type=\"file\"")))
                .andExpect(content().string(containsString("accept=\"audio/*\"")))
                .andExpect(content().string(containsString("type=\"submit\"")));
    }
    
    @Test
    void testAudioPageContainsPlaylistForm() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("playlist-form")))
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("name=\"description\"")))
                .andExpect(content().string(containsString("track-selection")))
                .andExpect(content().string(containsString("playlists-container")));
    }
    
    @Test
    void testAudioPageContainsNavigation() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/\"")))
                .andExpect(content().string(containsString("href=\"/chat\"")))
                .andExpect(content().string(containsString("Home")))
                .andExpect(content().string(containsString("Chat")));
    }
    
    @Test
    void testAudioPageContainsJavaScript() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<script")))
                .andExpect(content().string(containsString("setupUploadForm")))
                .andExpect(content().string(containsString("setupPlaylistForm")))
                .andExpect(content().string(containsString("loadTracks")))
                .andExpect(content().string(containsString("loadPlaylists")));
    }
    
    @Test
    void testAudioPageContainsStyling() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<style")))
                .andExpect(content().string(containsString("background: linear-gradient")))
                .andExpect(content().string(containsString(".btn")))
                .andExpect(content().string(containsString(".form-input")))
                .andExpect(content().string(containsString(".file-upload")));
    }
    
    @Test
    void testAudioPageContainsFontAwesome() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("font-awesome")))
                .andExpect(content().string(containsString("fas fa-upload")))
                .andExpect(content().string(containsString("fas fa-list")))
                .andExpect(content().string(containsString("fas fa-home")));
    }
    
    @Test
    void testAudioPageResponsiveDesign() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("@media (max-width: 768px)")))
                .andExpect(content().string(containsString("grid-template-columns: 1fr")))
                .andExpect(content().string(containsString("flex-direction: column")));
    }
    
    @Test
    void testAudioPageErrorHandling() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-container")))
                .andExpect(content().string(containsString("showAlert")))
                .andExpect(content().string(containsString("alert-success")))
                .andExpect(content().string(containsString("alert-error")));
    }
    
    @Test
    void testAudioPageLoadingStates() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/audio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("upload-loading")))
                .andExpect(content().string(containsString("playlist-loading")))
                .andExpect(content().string(containsString("spinner")))
                .andExpect(content().string(containsString("Uploading track")))
                .andExpect(content().string(containsString("Creating playlist")));
    }
}
