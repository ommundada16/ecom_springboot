package com.ecommerce.ecom_proj.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application (real security chain, JWT, JPA and the Flyway-managed schema on H2).
 * Every test runs in a transaction that is rolled back, so tests never see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    protected static final String ADMIN_EMAIL = "admin@shopease.com";
    protected static final String ADMIN_PASSWORD = "Admin@12345";
    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@shop.com";
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Registers a new customer account through the API and returns its JWT. */
    protected String registerUser(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    protected String adminToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    /** Creates a product as the admin and returns its id. */
    protected long createProduct(String adminToken, String name, String price, int stock, boolean available)
            throws Exception {
        Map<String, Object> product = Map.of(
                "name", name, "description", "Test product", "brand", "TestBrand", "price", price,
                "category", "Testing", "productAvailable", available, "stockQuantity", stock);
        String body = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(product)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
