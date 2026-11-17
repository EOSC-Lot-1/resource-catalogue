package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.dto.ProviderResources;

public interface ProviderResourcesService {

    /**
     * Get all the resources owned by the specific Provider, grouped by resource type.
     * Supported types: service, datasource, training-resource, interoperability-record, tool.
     * Services that are linked to a Datasource are returned only as Datasources.
     *
     * @param providerId   Provider ID
     * @param resourceType Optional resource type, if null all types are returned
     * @return {@link ProviderResources}, the types that were not requested are null
     */
    ProviderResources getProviderResources(String providerId, String resourceType);
}
