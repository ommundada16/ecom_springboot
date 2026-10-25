package com.ecommerce.ecom_proj.integration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIT extends IntegrationTestBase {

    @Autowired
    private EntityManager entityManager;

    private ResultActions placeOrder(String token, Object... productIdAndQuantityPairs) throws Exception {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (int i = 0; i < productIdAndQuantityPairs.length; i += 2) {
            items.add(Map.of("productId", productIdAndQuantityPairs[i], "quantity", productIdAndQuantityPairs[i + 1]));
        }
        return mockMvc.perform(post("/api/orders").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("items", items))));
    }

    private ResultActions product(long id) throws Exception {
        return mockMvc.perform(get("/api/products/" + id));
    }

    @Test
    void placeOrder_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("items", List.of(Map.of("productId", 1, "quantity", 1))))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_createsOrderWithCorrectTotalAndLines() throws Exception {
        String admin = adminToken();
        long phone = createProduct(admin, "Order Phone", "500.00", 10, true);
        long cable = createProduct(admin, "Order Cable", "12.50", 100, true);
        String token = registerUser(uniqueEmail());

        placeOrder(token, phone, 2, cable, 4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(1050.00))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productName").value("Order Phone"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(1000.00));
    }

    @Test
    void placeOrder_decrementsStock() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "Stock Item", "10.00", 10, true);

        placeOrder(registerUser(uniqueEmail()), id, 3).andExpect(status().isCreated());

        product(id).andExpect(jsonPath("$.stockQuantity").value(7));
    }

    @Test
    void placeOrder_mergesDuplicateLines() throws Exception {
        long id = createProduct(adminToken(), "Merged Item", "10.00", 10, true);

        placeOrder(registerUser(uniqueEmail()), id, 2, id, 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(50.00));
        product(id).andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void placeOrder_buyingTheLastUnits_succeedsThenNextOrderIsRejected() throws Exception {
        long id = createProduct(adminToken(), "Last Units", "10.00", 2, true);
        String token = registerUser(uniqueEmail());

        placeOrder(token, id, 2).andExpect(status().isCreated());
        product(id).andExpect(jsonPath("$.stockQuantity").value(0));

        placeOrder(token, id, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only 0")));
    }

    @Test
    void placeOrder_insufficientStock_returns409AndLeavesStockUntouched() throws Exception {
        String admin = adminToken();
        long plenty = createProduct(admin, "Plenty", "10.00", 50, true);
        long scarce = createProduct(admin, "Scarce", "10.00", 2, true);

        placeOrder(registerUser(uniqueEmail()), plenty, 5, scarce, 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("Scarce")));

        product(plenty).andExpect(jsonPath("$.stockQuantity").value(50));
        product(scarce).andExpect(jsonPath("$.stockQuantity").value(2));
    }

    @Test
    void placeOrder_unavailableProduct_returns409() throws Exception {
        long id = createProduct(adminToken(), "Unavailable", "10.00", 20, false);

        placeOrder(registerUser(uniqueEmail()), id, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("unavailable")));
    }

    @Test
    void placeOrder_unknownProduct_returns404() throws Exception {
        placeOrder(registerUser(uniqueEmail()), 999999L, 1)
                .andExpect(status().isNotFound());
    }

    @Test
    void placeOrder_emptyItems_returns400() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(registerUser(uniqueEmail())))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("items", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    @Test
    void placeOrder_zeroQuantity_returns400() throws Exception {
        placeOrder(registerUser(uniqueEmail()), 1L, 0)
                .andExpect(status().isBadRequest());
    }

    @Test
    void myOrders_listsOnlyTheCallersOrdersNewestFirst() throws Exception {
        long id = createProduct(adminToken(), "History Item", "10.00", 50, true);
        String alice = registerUser(uniqueEmail());
        String bob = registerUser(uniqueEmail());

        placeOrder(alice, id, 1).andExpect(status().isCreated());
        placeOrder(alice, id, 2).andExpect(status().isCreated());
        placeOrder(bob, id, 3).andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].items[0].quantity").value(2))
                .andExpect(jsonPath("$[1].items[0].quantity").value(1));
        mockMvc.perform(get("/api/orders").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void myOrders_isEmptyForANewUser() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", bearer(registerUser(uniqueEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getOrder_ownerCanReadIt() throws Exception {
        long id = createProduct(adminToken(), "Readable", "10.00", 5, true);
        String token = registerUser(uniqueEmail());
        long orderId = objectMapper.readTree(placeOrder(token, id, 1).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void getOrder_anotherUsersOrder_returns404() throws Exception {
        long id = createProduct(adminToken(), "Private", "10.00", 5, true);
        String owner = registerUser(uniqueEmail());
        long orderId = objectMapper.readTree(placeOrder(owner, id, 1).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(registerUser(uniqueEmail()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_adminCanReadAnyOrder() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "Admin Visible", "10.00", 5, true);
        String owner = registerUser(uniqueEmail());
        long orderId = objectMapper.readTree(placeOrder(owner, id, 1).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void deletingAProduct_keepsTheOrderHistoryWithItsSnapshot() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "Discontinued Item", "25.00", 5, true);
        String token = registerUser(uniqueEmail());
        long orderId = objectMapper.readTree(placeOrder(token, id, 2).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(delete("/api/products/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(50.00))
                .andExpect(jsonPath("$.items[0].productName").value("Discontinued Item"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(25.00))
                .andExpect(jsonPath("$.items[0].productId", nullValue()));
    }
}
