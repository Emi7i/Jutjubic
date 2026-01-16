package isa.jutjub.controller;

import isa.jutjub.dto.RegisterRequest;
import isa.jutjub.model.User;
import isa.jutjub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Convert RegisterRequest to User
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setName(registerRequest.getName());
            user.setSurname(registerRequest.getSurname());
            user.setAddress(registerRequest.getAddress());
            
            boolean success = authService.registerUser(user);
            if (success) return ResponseEntity.ok("User registered successfully");
            else return ResponseEntity.status(500).body("Failed to save user");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String usernameOrEmail = loginRequest.get("usernameOrEmail"); // can be username or email
            String password = loginRequest.get("password");

            if (usernameOrEmail == null || password == null || usernameOrEmail.isBlank() || password.isBlank()) {
                return ResponseEntity.badRequest().body("Username/email and password are required");
            }

            String token = authService.loginUser(usernameOrEmail, password);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        boolean activated = authService.activateUser(token);
        if (activated) {
            return ResponseEntity.ok("Account activated successfully. You can now log in.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired activation token.");
        }
    }

    @PostMapping("/manual-activate")
    public ResponseEntity<String> manualActivate(@RequestParam String username) {
        try {
            boolean activated = authService.manualActivateUser(username);
            if (activated) {
                return ResponseEntity.ok("Account activated successfully for user: " + username);
            } else {
                return ResponseEntity.badRequest().body("User not found: " + username);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to activate user: " + e.getMessage());
        }
    }
}
