package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createProviderBundle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API-layer integration tests for Provider resources.
 * <p>
 * Exercises the {@code ProviderCrudController} → {@code ResourceCrudController}
 * → {@code ResourceManager.save()} path via real HTTP (MockMvc) without JWT authentication.
 * The {@code no-auth} Spring profile disables the security filter chain.
 * <p>
 * The server always generates the resource ID via its configured id-prefix; the
 * tests capture the generated ID from the POST response and reuse it.
 * </p>
 */
class ProviderApiIntegrationTest extends BaseApiIntegrationTest {

    private static final String BASE_URL = "/providers";

    private String createdId;

    @Test
    @Order(1)
    void addProvider_returnsCreated() throws Exception {
        ProviderBundle bundle = createProviderBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdId = body.path("provider").path("id").asText();
        assertFalse(createdId.isBlank(), "Server must return a generated id");
    }

    @Test
    @Order(2)
    void getProvider_afterAdd_returnsOk() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider.id").value(createdId));
    }

    @Test
    @Order(3)
    void updateProvider_returnsOk() throws Exception {
        ProviderBundle bundle = createProviderBundle();
        bundle.getProvider().setId(createdId);
        bundle.getProvider().setName("Updated Provider Name");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider.name").value("Updated Provider Name"));
    }

    @Test
    @Order(4)
    void getProvider_afterUpdate_returnsUpdatedName() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider.name").value("Updated Provider Name"));
    }

    @Test
    @Order(5)
    void deleteProvider_returnsOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider.id").value(createdId));
    }

    @Test
    @Order(6)
    void getProvider_afterDelete_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addProvider_duplicate_returnsConflict() throws Exception {
        ProviderBundle bundle = createProviderBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("provider").path("id").asText();

        bundle.getProvider().setId(firstId);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateProvider_nonExistent_returnsNotFound() throws Exception {
        ProviderBundle bundle = createProviderBundle();
        bundle.getProvider().setId("non-existent-provider-id-999");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isNotFound());
    }
}
