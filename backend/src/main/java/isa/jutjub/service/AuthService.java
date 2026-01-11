package isa.jutjub.service;

import isa.jutjub.model.User;
import isa.jutjub.repository.UserRepository;
import isa.jutjub.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService; // inject email service

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public boolean registerUser(User user) throws IllegalArgumentException {
        if (user.getUsername() == null || user.getPassword() == null || user.getEmail() == null ||
                user.getUsername().isBlank() || user.getPassword().isBlank() || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Username, email and password are required");
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActivationToken(generateActivationToken(user));

        User savedUser = userRepository.save(user);

        emailService.sendActivationEmail(savedUser);

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
        if(!existingUser.isActive()) {
            throw new IllegalArgumentException("Account not activated, please check email");
        }


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

    public boolean activateUser(String token) {
        Optional<User> userOpt = userRepository.findByActivationToken(token);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        user.setActive(true);
        user.setActivationToken(null); // remove token after activation
        userRepository.save(user);
        return true;
    }

    private String generateActivationToken(User user) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String text = user.getUsername() + ":" + user.getEmail(); // unique per user
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate activation token", e);
        }
    }


}
