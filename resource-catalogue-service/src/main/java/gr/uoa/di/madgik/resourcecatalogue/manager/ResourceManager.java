package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.AbstractGenericService;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ResourceManager<T extends Identifiable> extends AbstractGenericService<T> implements ResourceService<T, Authentication> {

    private static final Logger logger = LogManager.getLogger(ResourceManager.class);

    @Lazy
    @Autowired
    private FieldValidator fieldValidator;

    public ResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public T get(String id) {
        return deserialize(whereID(id, true));
    }

    @Override
    public Resource getResource(String id) {
        return whereID(id, true);
    }

    @Override
    public Browsing<T> getAll(FacetFilter ff, Authentication auth) {
        ff.setBrowseBy(getBrowseBy());
        Browsing<T> browsing;
        try {
            browsing = getResults(ff);
        } catch (Exception e) {
            throw new ServiceException("Search error, check search parameters"); // check elastic status
        }
        return browsing;
    }

    @Override
    public Browsing<T> getMy(FacetFilter ff, Authentication auth) {
        return null;
    }

    @Override
    public T add(T t, Authentication auth) {
        if (exists(t)) {
            throw new ResourceException(String.format("%s with id = '%s' already exists!", resourceType.getName(), t.getId()), HttpStatus.CONFLICT);
        }
        String serialized = serialize(t);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        logger.debug("Adding Resource {}", t);
        return t;
    }

    @Override
    public T update(T t, Authentication auth) {
        Resource existing = whereID(t.getId(), true);
        existing.setPayload(serialize(t));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Resource {}", t);
        return t;
    }

    @Override
    public void delete(T t) {
        resourceService.deleteResource(whereID(t.getId(), true).getId());
        logger.debug("Deleting Resource {}", t);
    }

    @Override
    public Map<String, List<T>> getBy(String field) {
        return groupBy(field).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue()
                        .stream()
                        .map(resource -> deserialize(where(true, new SearchService.KeyValue("id", resource.getId()))))
                        .collect(Collectors.toList())));
    }

    @Override
    public List<T> getSome(String... ids) {
        return whereIDin(ids).stream().filter(Objects::nonNull).map(this::deserialize).collect(Collectors.toList());
    }

    @Override
    public T get(SearchService.KeyValue... keyValues) {
        return deserialize(where(true, keyValues));
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<T> delAll() {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        logger.info("Deleting all Resources");
        List<T> results = getAll(facetFilter, null).getResults();
        results.forEach(this::delete);
        return results;
    }

    @Override
    public T validate(T t) {
        logger.debug("Validating Resource '{}' using FieldValidator", t);
        try {
            fieldValidator.validate(t);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
        return t;
    }

    @Override
    public boolean exists(T t) {
        return whereID(t.getId(), false) != null;
    }

    public ParserService.ParserServiceTypes getCoreFormat() {
        return ParserService.ParserServiceTypes.XML;
    }

    protected String serialize(T t) {
        String ret = parserPool.serialize(t, getCoreFormat());
        if (ret.equals("failed")) {
            throw new ResourceException(String.format("Not a valid %s!", resourceType.getName()), HttpStatus.BAD_REQUEST);
        }
        return ret;
    }

    protected T deserialize(Resource resource) {
        return parserPool.deserialize(resource, typeParameterClass);
    }

    protected Map<String, List<Resource>> groupBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType.getName());
        ff.setQuantity(maxQuantity);
        return searchService.searchByCategory(ff, field);
    }

    protected List<Resource> whereIDin(String... ids) {
        return Stream.of(ids).map((String id) -> whereID(id, false)).collect(Collectors.toList());
    }

    protected Resource whereID(String id, boolean throwOnNull) {
        return where(throwOnNull, new SearchService.KeyValue("resource_internal_id", id));
    }

    protected Resource where(boolean throwOnNull, SearchService.KeyValue... keyValues) {
        Resource ret;
        ret = searchService.searchFields(resourceType.getName(), keyValues);
        if (throwOnNull && ret == null) {
            throw new ResourceException(String.format("%s does not exist!", resourceType.getName()), HttpStatus.NOT_FOUND);
        }
        return ret;
    }

}
