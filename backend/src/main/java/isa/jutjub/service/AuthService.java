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

    public String loginUser(String username, String password) throws IllegalArgumentException {
        Optional<User> existingUserOpt = userRepository.findByUsername(username);

        if (existingUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User existingUser = existingUserOpt.get();
        if (!passwordEncoder.matches(password, existingUser.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Generate JWT token
        return jwtUtil.generateToken(username);
    }
}
