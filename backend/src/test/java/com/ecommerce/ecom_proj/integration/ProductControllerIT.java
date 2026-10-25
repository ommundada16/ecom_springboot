package com.ecommerce.ecom_proj.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIT extends IntegrationTestBase {

    private Map<String, Object> validProduct() {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("name", "Test Phone");
        product.put("description", "A phone used in tests");
        product.put("brand", "Acme");
        product.put("price", "1999.99");
        product.put("category", "Mobile");
        product.put("releaseDate", "2024-06-01");
        product.put("productAvailable", true);
        product.put("stockQuantity", 12);
        return product;
    }

    // ---------- reading (public) ----------

    @Test
    void listProducts_isPublicAndPaginated() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(8)))
                .andExpect(jsonPath("$.page.size").value(8))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(19)))
                .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(3)));
    }

    @Test
    void listProducts_secondPageReturnsDifferentProducts() throws Exception {
        String first = mockMvc.perform(get("/api/products?page=0&size=5&sort=id"))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/products?page=1&size=5&sort=id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page.number").value(1))
                .andReturn().getResponse().getContentAsString();

        long firstId = objectMapper.readTree(first).get("content").get(0).get("id").asLong();
        long secondId = objectMapper.readTree(second).get("content").get(0).get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(secondId).isGreaterThan(firstId);
    }

    @Test
    void listProducts_canSortByPriceDescending() throws Exception {
        mockMvc.perform(get("/api/products?sort=price,desc&size=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Mahindra Thar"))
                .andExpect(jsonPath("$.content[0].price").value(1200000.00));
    }

    @Test
    void listProducts_canSortByNameAscending() throws Exception {
        mockMvc.perform(get("/api/products?sort=name,asc&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Amazon Echo Dot (5th Gen)"));
    }

    @Test
    void listProducts_unknownSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/api/products?sort=doesNotExist,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("doesNotExist")));
    }

    @Test
    void listProducts_filtersByCategoryIgnoringCase() throws Exception {
        mockMvc.perform(get("/api/products?category=laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].category", everyItem(equalToIgnoringCase("Laptop"))));
    }

    @Test
    void listProducts_unknownCategory_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/products?category=NoSuchCategory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void getProduct_returnsTheProduct() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tata Nexon"))
                .andExpect(jsonPath("$.releaseDate").value("2024-01-15"))
                .andExpect(jsonPath("$.productAvailable").value(true));
    }

    @Test
    void getProduct_missing_returns404WithJsonError() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("999999")))
                .andExpect(jsonPath("$.path").value("/api/products/999999"));
    }

    @Test
    void getProduct_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_matchesAcrossNameBrandAndCategoryCaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/products/search?keyword=SAMSUNG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Samsung Galaxy S23"));

        mockMvc.perform(get("/api/products/search?keyword=headphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void search_blankKeyword_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/products/search").param("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void search_treatsPercentAndUnderscoreAsPlainCharacters() throws Exception {
        mockMvc.perform(get("/api/products/search").param("keyword", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/products/search").param("keyword", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void search_findsProductsWhoseTextContainsAPercentSign() throws Exception {
        Map<String, Object> product = validProduct();
        product.put("name", "Juice 100% Natural");

        mockMvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(product)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/search").param("keyword", "100%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Juice 100% Natural"));
    }

    @Test
    void search_missingKeywordParameter_returns400() throws Exception {
        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isBadRequest());
    }

    // ---------- authorization ----------

    @Test
    void createProduct_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json(validProduct())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_asRegularUser_returns403() throws Exception {
        String token = registerUser(uniqueEmail());

        mockMvc.perform(post("/api/products").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(validProduct())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateProduct_asRegularUser_returns403() throws Exception {
        String token = registerUser(uniqueEmail());

        mockMvc.perform(put("/api/products/1").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(validProduct())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_asRegularUser_returns403AndKeepsTheProduct() throws Exception {
        String token = registerUser(uniqueEmail());

        mockMvc.perform(delete("/api/products/1").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/products/1")).andExpect(status().isOk());
    }

    @Test
    void deleteProduct_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/products/1")).andExpect(status().isUnauthorized());
    }

    // ---------- admin writes ----------

    @Test
    void createProduct_asAdmin_returns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(validProduct())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/products/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test Phone"))
                .andExpect(jsonPath("$.price").value(1999.99))
                .andExpect(jsonPath("$.stockQuantity").value(12));
    }

    @Test
    void createProduct_isThenReadableByAnonymousUsers() throws Exception {
        long id = createProduct(adminToken(), "Readable Thing", "10.00", 3, true);

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Readable Thing"));
    }

    @Test
    void createProduct_withoutOptionalFields_succeeds() throws Exception {
        Map<String, Object> minimal = Map.of("name", "Bare Minimum", "price", "5.00", "stockQuantity", 0);

        mockMvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(minimal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productAvailable").value(false))
                .andExpect(jsonPath("$.releaseDate").doesNotExist());
    }

    @Test
    void createProduct_invalidBody_returns400WithFieldErrors() throws Exception {
        Map<String, Object> invalid = validProduct();
        invalid.put("name", "   ");
        invalid.put("price", "-5");
        invalid.put("stockQuantity", -1);

        mockMvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.price").value("Price must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").value("Stock quantity cannot be negative"));
    }

    @Test
    void createProduct_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").exists());
    }

    @Test
    void createProduct_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed")));
    }

    @Test
    void updateProduct_asAdmin_replacesTheFields() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "Before", "10.00", 5, true);
        Map<String, Object> changes = validProduct();
        changes.put("name", "After");
        changes.put("stockQuantity", 99);

        mockMvc.perform(put("/api/products/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("After"));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.stockQuantity").value(99));
    }

    @Test
    void updateProduct_missing_returns404InsteadOfCreating() throws Exception {
        mockMvc.perform(put("/api/products/999999").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json(validProduct())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/999999")).andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_asAdmin_returns204ThenProductIsGone() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "Short Lived", "10.00", 5, true);

        mockMvc.perform(delete("/api/products/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_missing_returns404() throws Exception {
        mockMvc.perform(delete("/api/products/999999").header("Authorization", bearer(adminToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownPath_returns404Json() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void swaggerDocs_arePubliclyAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ShopEase API"))
                .andExpect(jsonPath("$.paths['/api/products']").exists())
                .andExpect(jsonPath("$.paths['/api/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }
}
