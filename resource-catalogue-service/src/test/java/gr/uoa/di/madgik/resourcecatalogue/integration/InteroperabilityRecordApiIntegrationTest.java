package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createInteroperabilityRecordBundle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API-layer integration tests for Interoperability Record resources.
 * <p>
 * Exercises the {@code InteroperabilityRecordCrudController} → {@code ResourceCrudController}
 * → {@code ResourceManager.save()} path via real HTTP (MockMvc) without JWT authentication.
 * The {@code no-auth} and {@code crud} profiles are active; the server generates the resource ID.
 * </p>
 */
class InteroperabilityRecordApiIntegrationTest extends BaseApiIntegrationTest {

    private static final String BASE_URL = "/interoperability-records";

    private String createdId;

    @Test
    @Order(1)
    void addInteroperabilityRecord_returnsCreated() throws Exception {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdId = body.path("interoperabilityRecord").path("id").asText();
        assertFalse(createdId.isBlank(), "Server must return a generated id");
    }

    @Test
    @Order(2)
    void getInteroperabilityRecord_afterAdd_returnsOk() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interoperabilityRecord.id").value(createdId));
    }

    @Test
    @Order(3)
    void updateInteroperabilityRecord_returnsOk() throws Exception {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.getInteroperabilityRecord().setId(createdId);
        bundle.getInteroperabilityRecord().setTitle("Updated Interoperability Record Title");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interoperabilityRecord.title").value("Updated Interoperability Record Title"));
    }

    @Test
    @Order(4)
    void getInteroperabilityRecord_afterUpdate_returnsUpdatedTitle() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interoperabilityRecord.title").value("Updated Interoperability Record Title"));
    }

    @Test
    @Order(5)
    void deleteInteroperabilityRecord_returnsOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interoperabilityRecord.id").value(createdId));
    }

    @Test
    @Order(6)
    void getInteroperabilityRecord_afterDelete_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addInteroperabilityRecord_duplicate_returnsConflict() throws Exception {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("interoperabilityRecord").path("id").asText();

        bundle.getInteroperabilityRecord().setId(firstId);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateInteroperabilityRecord_nonExistent_returnsNotFound() throws Exception {
        InteroperabilityRecordBundle bundle = createInteroperabilityRecordBundle();
        bundle.getInteroperabilityRecord().setId("non-existent-ir-id-999");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isNotFound());
    }
}
