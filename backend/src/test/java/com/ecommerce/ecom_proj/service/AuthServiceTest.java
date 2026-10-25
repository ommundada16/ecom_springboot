package com.ecommerce.ecom_proj.service;

import com.ecommerce.ecom_proj.dto.AuthResponse;
import com.ecommerce.ecom_proj.dto.LoginRequest;
import com.ecommerce.ecom_proj.dto.RegisterRequest;
import com.ecommerce.ecom_proj.exception.ConflictException;
import com.ecommerce.ecom_proj.model.Role;
import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.UserRepo;
import com.ecommerce.ecom_proj.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    @Test
    void register_hashesPasswordNormalizesEmailAndAssignsUserRole() {
        when(userRepo.existsByEmail("new@shop.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = service.register(new RegisterRequest("  New User ", "  New@Shop.com ", "password123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("HASHED");
        assertThat(saved.getValue().getEmail()).isEqualTo("new@shop.com");
        assertThat(saved.getValue().getName()).isEqualTo("New User");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepo.existsByEmail("taken@shop.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.register(new RegisterRequest("Name", "Taken@shop.com", "password123")));

        assertThat(ex.getMessage()).contains("already exists");
        verify(userRepo, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsTokenForTheUser() {
        User user = User.builder().id(5L).name("Ann").email("ann@shop.com").role(Role.ADMIN).build();
        when(userRepo.findByEmail("ann@shop.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = service.login(new LoginRequest(" Ann@Shop.com", "secret-pass"));

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("ann@shop.com", "secret-pass"));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.email()).isEqualTo("ann@shop.com");
    }

    @Test
    void login_badCredentials_propagatesAndNeverIssuesAToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("ann@shop.com", "wrong")));

        verify(jwtService, never()).generateToken(any());
    }
}
