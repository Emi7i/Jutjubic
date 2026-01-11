package isa.jutjub.controller;

import isa.jutjub.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import isa.jutjub.service.AuthService;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        try {
            boolean success = authService.registerUser(user);
            if (success) return ResponseEntity.ok("User registered successfully");
            else return ResponseEntity.status(500).body("Failed to save user");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            String token = authService.loginUser(user.getUsername(), user.getPassword());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

}
