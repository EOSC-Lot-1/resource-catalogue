package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createTrainingResourceBundle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API-layer integration tests for Training Resource resources.
 * <p>
 * Exercises the {@code TrainingResourceCrudController} → {@code ResourceCrudController}
 * → {@code ResourceManager.save()} path via real HTTP (MockMvc) without JWT authentication.
 * The server generates the resource ID; tests capture it from the POST response.
 * </p>
 */
class TrainingResourceApiIntegrationTest extends BaseApiIntegrationTest {

    private static final String BASE_URL = "/training-resources";

    private String createdId;

    @Test
    @Order(1)
    void addTrainingResource_returnsCreated() throws Exception {
        TrainingResourceBundle bundle = createTrainingResourceBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdId = body.path("trainingResource").path("id").asText();
        assertFalse(createdId.isBlank(), "Server must return a generated id");
    }

    @Test
    @Order(2)
    void getTrainingResource_afterAdd_returnsOk() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingResource.id").value(createdId));
    }

    @Test
    @Order(3)
    void updateTrainingResource_returnsOk() throws Exception {
        TrainingResourceBundle bundle = createTrainingResourceBundle();
        bundle.getTrainingResource().setId(createdId);
        bundle.getTrainingResource().setTitle("Updated Training Resource Title");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingResource.title").value("Updated Training Resource Title"));
    }

    @Test
    @Order(4)
    void getTrainingResource_afterUpdate_returnsUpdatedTitle() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingResource.title").value("Updated Training Resource Title"));
    }

    @Test
    @Order(5)
    void deleteTrainingResource_returnsOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingResource.id").value(createdId));
    }

    @Test
    @Order(6)
    void getTrainingResource_afterDelete_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addTrainingResource_duplicate_returnsConflict() throws Exception {
        TrainingResourceBundle bundle = createTrainingResourceBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("trainingResource").path("id").asText();

        bundle.getTrainingResource().setId(firstId);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateTrainingResource_nonExistent_returnsNotFound() throws Exception {
        TrainingResourceBundle bundle = createTrainingResourceBundle();
        bundle.getTrainingResource().setId("non-existent-tr-id-999");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isNotFound());
    }
}
