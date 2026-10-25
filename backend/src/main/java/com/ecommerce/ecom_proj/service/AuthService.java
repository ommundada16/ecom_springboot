package com.ecommerce.ecom_proj.service;

import com.ecommerce.ecom_proj.dto.AuthResponse;
import com.ecommerce.ecom_proj.dto.LoginRequest;
import com.ecommerce.ecom_proj.dto.RegisterRequest;
import com.ecommerce.ecom_proj.exception.ConflictException;
import com.ecommerce.ecom_proj.exception.ResourceNotFoundException;
import com.ecommerce.ecom_proj.model.Role;
import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.UserRepo;
import com.ecommerce.ecom_proj.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepo.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = userRepo.save(User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        // Throws BadCredentialsException for an unknown email or a wrong password.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(),
                user.getRole().name());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
