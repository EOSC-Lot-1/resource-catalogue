package gr.uoa.di.madgik.resourcecatalogue.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

/**
 * Base class for API-layer integration tests. Unlike {@link BaseIntegrationTest}, this class boots
 * the full web layer (MockMvc) and activates the {@code no-auth} Spring profile so that the
 * security filter chain permits all requests without a real JWT token.
 * <p>
 * These tests exercise the {@code ResourceCrudController} → {@code ResourceManager.save()} path,
 * which is the actual code path executed when the application is called through its REST API.
 */
@SpringBootTest(
        properties = {"spring.profiles.active=test,no-auth,crud"},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ImportTestcontainers(IntegrationTestConfig.class)
@Import(TestSecurityConfig.class)
public abstract class BaseApiIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private VocabularyService vocabularyService;

    @BeforeAll
    void loadVocabulariesFromFile() throws IOException {
        if (vocabularyService.getAll(new FacetFilter()).getTotal() == 0) {
            ObjectMapper mapper = new ObjectMapper();
            ClassLoader classLoader = getClass().getClassLoader();
            List<Vocabulary> vocabularies = mapper.readValue(
                    classLoader.getResource("vocabularies.json"),
                    new TypeReference<>() {
                    }
            );
            vocabularyService.addBulk(vocabularies, null);
        }
    }
}
