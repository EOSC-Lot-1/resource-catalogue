package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.databind.JsonNode;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createDatasourceBundle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API-layer integration tests for Datasource resources.
 * <p>
 * Exercises the {@code DatasourceCrudController} → {@code ResourceCrudController}
 * → {@code ResourceManager.save()} path via real HTTP (MockMvc) without JWT authentication.
 * The server generates the resource ID; tests capture it from the POST response.
 * </p>
 */
class DatasourceApiIntegrationTest extends BaseApiIntegrationTest {

    private static final String BASE_URL = "/datasources";

    private String createdId;

    @Test
    @Order(1)
    void addDatasource_returnsCreated() throws Exception {
        DatasourceBundle bundle = createDatasourceBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdId = body.path("datasource").path("id").asText();
        assertFalse(createdId.isBlank(), "Server must return a generated id");
    }

    @Test
    @Order(2)
    void getDatasource_afterAdd_returnsOk() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasource.id").value(createdId));
    }

    @Test
    @Order(3)
    void updateDatasource_returnsOk() throws Exception {
        DatasourceBundle bundle = createDatasourceBundle();
        bundle.getDatasource().setId(createdId);
        bundle.getDatasource().setDatasourceClassification("ds_classification-aggregators");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasource.datasourceClassification").value("ds_classification-aggregators"));
    }

    @Test
    @Order(4)
    void getDatasource_afterUpdate_returnsUpdatedClassification() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasource.datasourceClassification").value("ds_classification-aggregators"));
    }

    @Test
    @Order(5)
    void deleteDatasource_returnsOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasource.id").value(createdId));
    }

    @Test
    @Order(6)
    void getDatasource_afterDelete_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addDatasource_duplicate_returnsConflict() throws Exception {
        DatasourceBundle bundle = createDatasourceBundle();

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("datasource").path("id").asText();

        bundle.getDatasource().setId(firstId);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateDatasource_nonExistent_returnsNotFound() throws Exception {
        DatasourceBundle bundle = createDatasourceBundle();
        bundle.getDatasource().setId("non-existent-datasource-id-999");

        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundle)))
                .andExpect(status().isNotFound());
    }
}