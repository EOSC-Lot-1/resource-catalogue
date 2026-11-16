package gr.uoa.di.madgik.resourcecatalogue.service;

import java.util.List;
import java.util.Map;

public interface ProviderResourcesService {

    /**
     * Get all the resources owned by the specific Provider, grouped by resource type.
     * Supported types: service, datasource, training-resource, interoperability-record, tool.
     * Services that are linked to a Datasource are returned only as Datasources.
     *
     * @param providerId   Provider ID
     * @param resourceType Optional resource type, if null all types are returned
     * @return resource type -&gt; list of the Bundles of that type
     */
    Map<String, List<?>> getProviderResources(String providerId, String resourceType);
}
