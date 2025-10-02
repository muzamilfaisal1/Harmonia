package edu.cqu.coit13235.musicchat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.cqu.coit13235.musicchat.domain.ChatMessage;
import edu.cqu.coit13235.musicchat.service.ChatService;

@Controller
public class HomeController {

    private final ChatService chatService;

    @Autowired
    public HomeController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String home() {
        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            // User is logged in, redirect to dashboard
            return "redirect:/dashboard";
        }
        // User is not logged in, show home page
        return "index";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        // Get all messages for display
        List<ChatMessage> messages = chatService.getConversation();
        model.addAttribute("messages", messages);
        return "chat";
    }

    @GetMapping("/audio")
    public String audio() {
        return "audio";
    }
    
    @GetMapping("/playlists")
    public String playlists() {
        return "playlists";
    }

    // Removed POST /chat endpoint - now using /api/chat/messages with authentication
}
