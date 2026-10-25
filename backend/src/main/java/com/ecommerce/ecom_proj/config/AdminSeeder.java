package com.ecommerce.ecom_proj.config;

import com.ecommerce.ecom_proj.model.Role;
import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public AdminSeeder(UserRepo userRepo, PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()
                || admin.password() == null || admin.password().isBlank()) {
            return;
        }
        String email = admin.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) {
            return;
        }
        userRepo.save(User.builder()
                .name(admin.name())
                .email(email)
                .password(passwordEncoder.encode(admin.password()))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("Created admin account {}", email);
    }
}
