package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.ProviderResources;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ProviderResourcesManager implements ProviderResourcesService {

    private static final String SERVICE = "service";
    private static final String DATASOURCE = "datasource";
    private static final String TRAINING_RESOURCE = "training-resource";
    private static final String INTEROPERABILITY_RECORD = "interoperability-record";
    private static final String TOOL = "tool";
    private static final List<String> TYPES = List.of(SERVICE, DATASOURCE, TRAINING_RESOURCE, INTEROPERABILITY_RECORD, TOOL);

    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final ToolService toolService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public ProviderResourcesManager(@Lazy ProviderService providerService,
                                    ServiceBundleService<ServiceBundle> serviceBundleService,
                                    DatasourceService datasourceService,
                                    TrainingResourceService trainingResourceService,
                                    ToolService toolService,
                                    InteroperabilityRecordService interoperabilityRecordService) {
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.toolService = toolService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @Override
    public ProviderResources getProviderResources(String providerId, String resourceType) {
        if (resourceType != null && !TYPES.contains(resourceType)) {
            throw new ValidationException(String.format("Unsupported resourceType '%s'. Supported: %s", resourceType, TYPES));
        }
        providerService.get(providerId); // throws if the Provider does not exist

        ProviderResources result = new ProviderResources();
        boolean needsServices = resourceType == null || SERVICE.equals(resourceType) || DATASOURCE.equals(resourceType);
        List<ServiceBundle> services = needsServices ? providerServices(providerId) : List.of();
        List<DatasourceBundle> datasources = List.of();
        if (needsServices) {
            Set<String> serviceIds = services.stream().map(ServiceBundle::getId).collect(Collectors.toSet());
            datasources = datasourceService.getAll(filter()).getResults().stream()
                    .filter(d -> serviceIds.contains(d.getDatasource().getServiceId()))
                    .collect(Collectors.toList());
        }

        if (resourceType == null || SERVICE.equals(resourceType)) {
            // Services that have a Datasource are represented by that Datasource
            Set<String> withDatasource = datasources.stream().map(d -> d.getDatasource().getServiceId()).collect(Collectors.toSet());
            result.setServices(services.stream().filter(s -> !withDatasource.contains(s.getId())).collect(Collectors.toList()));
        }
        if (resourceType == null || DATASOURCE.equals(resourceType)) {
            result.setDatasources(datasources);
        }
        if (resourceType == null || TRAINING_RESOURCE.equals(resourceType)) {
            FacetFilter ff = filter();
            ff.addFilter("resource_organisation", providerId);
            result.setTrainingResources(trainingResourceService.getAll(ff).getResults());
        }
        if (resourceType == null || INTEROPERABILITY_RECORD.equals(resourceType)) {
            FacetFilter ff = filter();
            ff.addFilter("provider_id", providerId);
            result.setInteroperabilityRecords(interoperabilityRecordService.getAll(ff).getResults());
        }
        if (resourceType == null || TOOL.equals(resourceType)) {
            // the tool index has no provider field, so the resourceProvider of the payload is checked here
            result.setTools(toolService.getAll(filter()).getResults().stream()
                    .filter(t -> providerId.equals(t.getTool().getResourceProvider()))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private List<ServiceBundle> providerServices(String providerId) {
        FacetFilter ff = filter();
        ff.addFilter("resource_organisation", providerId);
        return serviceBundleService.getAll(ff).getResults();
    }

    // the one-argument getAll does not restrict by visibility, same as the generic CRUD endpoints
    private FacetFilter filter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        return ff;
    }
}
