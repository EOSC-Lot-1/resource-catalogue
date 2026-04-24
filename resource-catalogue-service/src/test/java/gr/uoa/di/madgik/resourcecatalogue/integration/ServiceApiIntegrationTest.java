package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * API-layer integration tests for Service resources.
 * <p>
 * Exercises the {@code ServiceCrudController} → {@code ResourceCrudController}
 * → {@code ResourceManager.save()} path via real HTTP (MockMvc) without JWT authentication.
 * The {@code no-auth} Spring profile disables the security filter chain.
 * <p>
 * The server always generates the resource ID via its configured id-prefix; the
 * tests capture the generated ID from the POST response and reuse it for
 * subsequent ordered operations.
 * </p>
 */
class ServiceApiIntegrationTest extends BaseApiIntegrationTest {

    private static final String BASE_URL = "/services";

    /** Populated by {@link #addService_returnsCreated()} and shared with later ordered tests. */
    private String createdId;

    // -------------------------------------------------------------------------
    // Ordered CRUD tests
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void addService_returnsCreated() throws Exception {
        ServiceBundle bundle = createServiceBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdId = body.path("service").path("id").asText();
        assertFalse(createdId.isBlank(), "Server must return a generated id");
    }

    @Test
    @Order(2)
    void getService_afterAdd_returnsOk() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service.id").value(createdId));
    }

    @Test
    @Order(3)
    void updateService_returnsOk() throws Exception {
        ServiceBundle bundle = createServiceBundle();
        bundle.getService().setId(createdId);
        bundle.getService().setName("Updated Service Name");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service.name").value("Updated Service Name"));
    }

    @Test
    @Order(4)
    void getService_afterUpdate_returnsUpdatedName() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service.name").value("Updated Service Name"));
    }

    @Test
    @Order(5)
    void deleteService_returnsOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service.id").value(createdId));
    }

    @Test
    @Order(6)
    void getService_afterDelete_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Independent validation tests
    // -------------------------------------------------------------------------

    @Test
    void addService_duplicate_returnsConflict() throws Exception {
        ServiceBundle bundle = createServiceBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("service").path("id").asText();

        bundle.getService().setId(firstId);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateService_nonExistent_returnsNotFound() throws Exception {
        ServiceBundle bundle = createServiceBundle();
        bundle.getService().setId("non-existent-service-id-999");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isNotFound());
    }
}
