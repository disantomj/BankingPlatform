package com.example.bankingplatform.user;

import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.security.JwtProvider;
import com.example.bankingplatform.user.dto.LoginRequest;
import com.example.bankingplatform.user.dto.SignupRequest;
import com.example.bankingplatform.user.dto.AuthResponse;
import com.example.bankingplatform.user.exception.UserAlreadyExistsException;
import com.example.bankingplatform.user.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.auditService = auditService;
    }

    /**
     * Register a new user
     */
    public AuthResponse signup(SignupRequest signupRequest) {
        // Validate user doesn't already exist
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + signupRequest.getUsername());
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + signupRequest.getEmail());
        }

        // Create new user
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole(signupRequest.getRole() != null ? signupRequest.getRole() : Role.CUSTOMER);

        // Save user
        User savedUser = userRepository.save(user);

        // Log user creation
        auditService.logEntityAction(
            savedUser.getId(),
            savedUser.getUsername(),
            AuditAction.USER_CREATED,
            AuditSeverity.MEDIUM,
            "User account created successfully",
            "User",
            savedUser.getId().toString()
        );

        // Generate JWT token
        String token = jwtProvider.generateToken(savedUser.getUsername(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getUsername(), savedUser.getRole().name());
    }

    /**
     * Authenticate user and return JWT token
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            // Find user by username
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                // Log failed login attempt
                auditService.logFailedAction(
                    user.getId(),
                    user.getUsername(),
                    AuditAction.LOGIN_FAILED,
                    AuditSeverity.HIGH,
                    "Failed login attempt - incorrect password",
                    "Invalid password provided"
                );
                throw new InvalidCredentialsException("Invalid username or password");
            }

            // Log successful login
            auditService.logAction(
                user.getId(),
                user.getUsername(),
                AuditAction.LOGIN,
                AuditSeverity.LOW,
                "User logged in successfully"
            );

            // Generate JWT token
            String token = jwtProvider.generateToken(user.getUsername(), user.getRole().name());

            return new AuthResponse(token, user.getUsername(), user.getRole().name());
        } catch (InvalidCredentialsException e) {
            // Log failed login attempt with unknown user
            auditService.logFailedAction(
                null,
                loginRequest.getUsername(),
                AuditAction.LOGIN_FAILED,
                AuditSeverity.CRITICAL,
                "Failed login attempt - user not found",
                "User '" + loginRequest.getUsername() + "' not found"
            );
            throw e;
        }
    }

    /**
     * Find user by username
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get user profile information (without sensitive data)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}