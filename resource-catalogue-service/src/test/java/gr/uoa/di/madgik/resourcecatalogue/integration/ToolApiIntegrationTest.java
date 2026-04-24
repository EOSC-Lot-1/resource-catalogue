package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createToolBundle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API-layer integration tests for Tool resources.
 * <p>
 * Exercises the {@code ToolCrudController} → {@code ResourceCrudController}
 * → {@code ResourceManager.save()} path via real HTTP (MockMvc) without JWT authentication.
 * The {@code no-auth} Spring profile disables the security filter chain.
 * <p>
 * The server always generates the resource ID via its configured id-prefix; the
 * tests capture the generated ID from the POST response and reuse it for
 * subsequent ordered operations.
 * </p>
 */
class ToolApiIntegrationTest extends BaseApiIntegrationTest {

    private static final String BASE_URL = "/tools";

    /** Populated by {@link #addTool_returnsCreated()} and shared with later ordered tests. */
    private String createdId;

    // -------------------------------------------------------------------------
    // Ordered CRUD tests
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void addTool_returnsCreated() throws Exception {
        ToolBundle bundle = createToolBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdId = body.path("tool").path("id").asText();
        assertFalse(createdId.isBlank(), "Server must return a generated id");
    }

    @Test
    @Order(2)
    void getTool_afterAdd_returnsOk() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool.id").value(createdId));
    }

    @Test
    @Order(3)
    void updateTool_returnsOk() throws Exception {
        ToolBundle bundle = createToolBundle();
        bundle.getTool().setId(createdId);
        bundle.getTool().setName("Updated Tool Name");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool.name").value("Updated Tool Name"));
    }

    @Test
    @Order(4)
    void getTool_afterUpdate_returnsUpdatedName() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool.name").value("Updated Tool Name"));
    }

    @Test
    @Order(5)
    void deleteTool_returnsOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool.id").value(createdId));
    }

    @Test
    @Order(6)
    void getTool_afterDelete_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Independent validation tests
    // -------------------------------------------------------------------------

    @Test
    void addTool_duplicate_returnsConflict() throws Exception {
        ToolBundle bundle = createToolBundle();

        // First POST — server generates an ID
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("tool").path("id").asText();

        // Second POST with the same generated ID set → expect 409 Conflict
        bundle.getTool().setId(firstId);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateTool_nonExistent_returnsNotFound() throws Exception {
        ToolBundle bundle = createToolBundle();
        bundle.getTool().setId("non-existent-tool-id-999");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isNotFound());
    }
}
