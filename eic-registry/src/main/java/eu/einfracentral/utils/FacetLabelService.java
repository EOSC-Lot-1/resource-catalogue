package eu.einfracentral.utils;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Value;
import org.apache.commons.collections.list.TreeList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class FacetLabelService {

    private static final Logger logger = LogManager.getLogger(FacetLabelService.class);
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final VocabularyService vocabularyService;
    private final RestHighLevelClient client;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    FacetLabelService(ProviderService<ProviderBundle, Authentication> providerService,
                      VocabularyService vocabularyService,
                      RestHighLevelClient client) {
        this.providerService = providerService;
        this.vocabularyService = vocabularyService;
        this.client = client;
    }

    public List<Facet> generateLabels(List<Facet> facets) {
        Map<String, String> vocabularyValues = null;
        try {
            vocabularyValues = getIdNameFields();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        for (Facet facet : facets) {
            Map<String, String> finalVocabularyValues = vocabularyValues;
            facet.getValues().forEach(value -> value.setLabel(getLabelElseKeepValue(value.getValue(), finalVocabularyValues)));
        }
        return facets;
    }

    private Map<String, String> getIdNameFields() throws IOException, ElasticsearchStatusException {
        Map<String, String> idNameMap = new TreeMap<>();

        final Scroll scroll = new Scroll(TimeValue.timeValueSeconds(1L));
        SearchRequest searchRequest = new SearchRequest("resourceTypes");
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .size(10000)
                .docValueField("resource_internal_id")
                .docValueField("name")
                .docValueField("title")
                .fetchSource(false)
                .explain(true);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        while (searchHits != null && searchHits.length > 0) {

            for (SearchHit hit : searchHits) {
                if (hit.getFields().containsKey("resource_internal_id")) {
                    String id = (String) hit.getFields().get("resource_internal_id").getValues().get(0);
                    if (hit.getFields().containsKey("name")) {
                        idNameMap.put(id, (String) hit.getFields().get("name").getValues().get(0));
                    } else if (hit.getFields().containsKey("title")) {
                        idNameMap.put(id, (String) hit.getFields().get("title").getValues().get(0));
                    }
                } else {
                    logger.error("Could not create id - name value. \nHit: {}", hit);
                }
            }

            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();
        }

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        boolean succeeded = clearScrollResponse.isSucceeded();
        if (!succeeded) {
            logger.error("clear scroll request failed...");
        }

        return idNameMap;
    }

    String toProperCase(String str, String delimiter, String newDelimiter) {
        if (str.equals("")) {
            str = "-";
        }
        StringJoiner joiner = new StringJoiner(newDelimiter);
        for (String s : str.split(delimiter)) {
            try {
                String s1;
                s1 = s.substring(0, 1).toUpperCase() + s.substring(1);
                joiner.add(s1);
            } catch (IndexOutOfBoundsException e) {
                return str;
            }
        }
        return joiner.toString();
    }

    String getLabelElseKeepValue(String value, Map<String, String> labels) {
        String ret = labels.get(value);
        if (ret == null) {
            ret = toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        return ret;
    }

    // TODO: remove this method
    @Deprecated
    @SuppressWarnings("unchecked")
    public List<Facet> createLabels(List<Facet> facets) {
        List<Facet> enrichedFacets = new TreeList(); // unchecked warning here
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> allProviders = providerService.getAll(ff, null).getResults();
        Map<String, String> providerNames = new TreeMap<>();
        allProviders.forEach(p -> providerNames.putIfAbsent(p.getId(), p.getProvider().getName()));

        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();

        Facet superCategories;
        Facet categories;
        Facet scientificDomains;

        for (Facet facet : facets) {
            if (facet.getField().equals("subcategories")) {
                categories = createCategoriesFacet(facet);
                superCategories = createSupercategoriesFacet(categories);

                enrichedFacets.add(superCategories);
                enrichedFacets.add(categories);
            }
            if (facet.getField().equals("scientific_subdomains")) {
                scientificDomains = createScientificDomainsFacet(facet);
                enrichedFacets.add(scientificDomains);
            }
            for (Value value : facet.getValues()) {

                switch (facet.getField()) {
                    case "resource_providers":
                    case "resource_organisation":
                        value.setLabel(providerNames.get(value.getValue()));
                        break;

                    default:
                        if (allVocabularies.containsKey(value.getValue())) {
                            value.setLabel(allVocabularies.get(value.getValue()).getName());
                        } else {
                            try {
                                value.setLabel(toProperCase(toProperCase(value.getValue(), "-", "-"), "_", " "));
                            } catch (StringIndexOutOfBoundsException e) {
                                logger.debug(e);
                            }
                        }
                }
            }
        }
        enrichedFacets.addAll(facets);

        return enrichedFacets;
    }

    // TODO: remove this method
    @Deprecated
    Facet createCategoriesFacet(Facet subcategories) {
        List<Value> categoriesValues = new ArrayList<>();

        Map<String, Vocabulary> categoriesMap = new TreeMap<>();

        for (Value value : subcategories.getValues()) {
            Vocabulary parent = vocabularyService.getParent(value.getValue());
            if (parent != null) {
                categoriesMap.putIfAbsent(parent.getId(), parent);
            }
        }

        for (Vocabulary category : categoriesMap.values()) {
            Value value = new Value();
            value.setValue(category.getId());
            value.setLabel(category.getName());
            categoriesValues.add(value);
        }

        Facet categories = new Facet();
        categories.setField("categories");
        categories.setLabel("Categories");
        categories.setValues(categoriesValues);
        return categories;
    }

    // TODO: remove this method
    @Deprecated
    Facet createSupercategoriesFacet(Facet categories) {
        List<Value> superCategoriesValues = new ArrayList<>();

        Map<String, Vocabulary> categoriesMap = new TreeMap<>();

        for (Value value : categories.getValues()) {
            Vocabulary parent = vocabularyService.getParent(value.getValue());
            if (parent != null) {
                categoriesMap.putIfAbsent(parent.getId(), parent);
            }
        }

        for (Vocabulary category : categoriesMap.values()) {
            Value value = new Value();
            value.setValue(category.getId());
            value.setLabel(category.getName());
            superCategoriesValues.add(value);
        }

        Facet superCategories = new Facet();
        superCategories.setField("supercategories");
        superCategories.setLabel("Supercategories");
        superCategories.setValues(superCategoriesValues);
        return superCategories;
    }

    // TODO: remove this method
    @Deprecated
    Facet createScientificDomainsFacet(Facet scientificSubdomains) {
        List<Value> scientificDomainsValues = new ArrayList<>();

        Map<String, Vocabulary> categoriesMap = new TreeMap<>();

        for (Value value : scientificSubdomains.getValues()) {
            Vocabulary parent = vocabularyService.getParent(value.getValue());
            if (parent != null) {
                categoriesMap.putIfAbsent(parent.getId(), parent);
            }
        }

        for (Vocabulary category : categoriesMap.values()) {
            Value value = new Value();
            value.setValue(category.getId());
            value.setLabel(category.getName());
            scientificDomainsValues.add(value);
        }

        Facet scientificDomains = new Facet();
        scientificDomains.setField("scientific_domains");
        scientificDomains.setLabel("Scientific Domains");
        scientificDomains.setValues(scientificDomainsValues);
        return scientificDomains;
    }
}
