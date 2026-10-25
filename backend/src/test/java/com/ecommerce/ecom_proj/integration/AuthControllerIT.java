package com.ecommerce.ecom_proj.integration;

import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends IntegrationTestBase {

    @Autowired
    private UserRepo userRepo;

    private org.springframework.test.web.servlet.ResultActions register(Map<String, String> body) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    @Test
    void register_createsUserAccountAndReturnsToken() throws Exception {
        String email = uniqueEmail();

        register(Map.of("name", "Ann", "email", email, "password", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Ann"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_storesABCryptHashNotThePlainPassword() throws Exception {
        String email = uniqueEmail();
        register(Map.of("name", "Ann", "email", email, "password", PASSWORD)).andExpect(status().isCreated());

        User stored = userRepo.findByEmail(email).orElseThrow();

        assertThat(stored.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void register_duplicateEmail_returns409EvenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        register(Map.of("name", "Ann", "email", email, "password", PASSWORD)).andExpect(status().isCreated());

        register(Map.of("name", "Other", "email", email.toUpperCase(), "password", PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        register(Map.of("name", "", "email", "not-an-email", "password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_cannotSelfAssignAdminRole() throws Exception {
        String email = uniqueEmail();

        register(Map.of("name", "Sneaky", "email", email, "password", PASSWORD, "role", "ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        String email = uniqueEmail();
        registerUser(email);

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_isCaseInsensitiveOnEmail() throws Exception {
        String email = uniqueEmail();
        registerUser(email);

        login(email.toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        registerUser(email);

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_unknownEmail_returnsSame401AsWrongPassword() throws Exception {
        login("nobody@shop.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_seededAdminHasAdminRole() throws Exception {
        login(ADMIN_EMAIL, ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401Json() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_isAccessible() throws Exception {
        String token = registerUser(uniqueEmail());

        mockMvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
