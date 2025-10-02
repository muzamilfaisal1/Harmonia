package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.domain.User;
import edu.cqu.coit13235.musicchat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for authentication-related endpoints.
 * Handles signup, signin, and dashboard functionality.
 */
@Controller
public class AuthController {
    
    private final UserService userService;
    
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Display the signup form.
     * 
     * @return signup template
     */
    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }
    
    /**
     * Process signup form submission.
     * 
     * @param username the username
     * @param email the email
     * @param password the password
     * @param redirectAttributes for flash messages
     * @return redirect to signin page
     */
    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                        @RequestParam String email,
                        @RequestParam String password,
                        RedirectAttributes redirectAttributes) {
        
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Username is required");
            return "redirect:/signup";
        }
        
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email is required");
            return "redirect:/signup";
        }
        
        if (password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Password is required");
            return "redirect:/signup";
        }
        
        // Check if username already exists
        if (userService.usernameExists(username)) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/signup";
        }
        
        // Check if email already exists
        if (userService.emailExists(email)) {
            redirectAttributes.addFlashAttribute("error", "Email already exists");
            return "redirect:/signup";
        }
        
        try {
            // Create and save user
            User user = new User(username, email, password);
            userService.save(user);
            redirectAttributes.addFlashAttribute("success", "Account created successfully! Please sign in.");
            return "redirect:/signin";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating account: " + e.getMessage());
            return "redirect:/signup";
        }
    }
    
    /**
     * Display the signin form.
     * 
     * @return signin template
     */
    @GetMapping("/signin")
    public String signinForm() {
        return "signin";
    }
    
    /**
     * Display the dashboard for authenticated users.
     * 
     * @param model for passing data to template
     * @return dashboard template
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        return "dashboard";
    }
}
