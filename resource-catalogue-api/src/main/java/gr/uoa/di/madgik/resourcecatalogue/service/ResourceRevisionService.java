package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceRevision;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceRevisionBundle;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ResourceRevisionService extends ResourceService<ResourceRevisionBundle>, BundleOperations<ResourceRevisionBundle> {

    /**
     * Add a new ResourceRevision
     *
     * @param resource    ResourceRevision
     * @param auth        Authentication
     * @return {@link   ResourceRevisionBundle}
     */
    ResourceRevisionBundle add(ResourceRevisionBundle resource, Authentication auth);

    /**
     * Update a ResourceRevision of the EOSC Catalogue.
     *
     * @param resource ResourceRevision
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link   ResourceRevisionBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    ResourceRevisionBundle update(ResourceRevisionBundle resource, String comment, Authentication auth) throws ResourceNotFoundException;


    /**
     * Returns the ResourceRevision with the specified ID
     *
     * @param id          ResourceRevision ID
     * @param catalogueId Catalogue ID
     * @return {@link   ResourceRevisionBundle}
     */
    ResourceRevisionBundle get(String id, String catalogueId);

}
