package isa.jutjub.service;

import isa.jutjub.model.User;
import isa.jutjub.repository.UserRepository;
import isa.jutjub.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public boolean registerUser(User user) throws IllegalArgumentException {
        if (user.getUsername() == null || user.getPassword() == null ||
                user.getUsername().isBlank() || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        return userRepository.findById(savedUser.getId()).isPresent();
    }

    public String loginUser(String usernameOrEmail, String password) throws IllegalArgumentException {
        System.out.println("Login attempt for username/email: " + usernameOrEmail);
        
        // Try to find user by username first, then by email
        Optional<User> existingUserOpt = userRepository.findByUsername(usernameOrEmail);
        if (existingUserOpt.isEmpty()) {
            existingUserOpt = userRepository.findByEmail(usernameOrEmail);
        }

        if (existingUserOpt.isEmpty()) {
            System.out.println("User not found: " + usernameOrEmail);
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        User existingUser = existingUserOpt.get();
        System.out.println("User found: " + existingUser.getUsername());
        System.out.println("Stored password hash: " + existingUser.getPassword());
        System.out.println("Input password: " + password);
        System.out.println("Password matches: " + passwordEncoder.matches(password, existingUser.getPassword()));
        
        if (!passwordEncoder.matches(password, existingUser.getPassword())) {
            System.out.println("Password mismatch for user: " + existingUser.getUsername());
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(existingUser.getUsername());
        System.out.println("Generated token for user: " + existingUser.getUsername());
        return token;
    }
}
