package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InteroperabilityRecordService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceInteroperabilityRecordService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("resourceInteroperabilityRecordManager")
public class ResourceInteroperabilityRecordManager extends ResourceManager<ResourceInteroperabilityRecordBundle>
        implements ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordManager.class);
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;

    public ResourceInteroperabilityRecordManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                                                 ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                                 TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                                                 InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                                                 SecurityService securityService, ProviderResourcesCommonMethods commonMethods,
                                                 PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager) {
        super(ResourceInteroperabilityRecordBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
    }

    @Override
    public String getResourceType() {
        return "resource_interoperability_record";
    }

    @Override
    public ResourceInteroperabilityRecordBundle validate(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType) {
        String resourceId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId();
        String catalogueId = resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId();

        ResourceInteroperabilityRecordBundle existing = getResourceInteroperabilityRecordByResourceId(resourceId, catalogueId, securityService.getAdminAccess());
        if (existing != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Resource " +
                                "Interoperability Record registered, with id: [%s]", resourceId, catalogueId, existing.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("datasource")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, datasourceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else{
            throw new ValidationException("Field 'resourceType' should be either 'service', 'datasource' or 'training_resource'");
        }

        super.validate(resourceInteroperabilityRecordBundle);
        return checkIfEachInteroperabilityRecordIsApproved(resourceInteroperabilityRecordBundle);
    }

    @Override
    public ResourceInteroperabilityRecordBundle add(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, String resourceType, Authentication auth) {
        validate(resourceInteroperabilityRecordBundle, resourceType);
        commonMethods.checkRelatedResourceIDsConsistency(resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new ResourceInteroperabilityRecord: {}", auth, resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(resourceInteroperabilityRecordBundle, auth);
        resourceInteroperabilityRecordBundle.setLoggingInfo(loggingInfoList);
        resourceInteroperabilityRecordBundle.setLatestOnboardingInfo(loggingInfoList.get(0));

        // active
        resourceInteroperabilityRecordBundle.setActive(true);

        ResourceInteroperabilityRecordBundle ret;
        ret = super.add(resourceInteroperabilityRecordBundle, null);
        logger.debug("Adding ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

        return ret;
    }

    public ResourceInteroperabilityRecordBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Resource Interoperability Record with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    public Resource getResource(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("%s_id = \"%s\"  AND catalogue_id = \"%s\"", resourceType.getName(), id, catalogueId),
                        resourceType.getName(), maxQuantity, 0, "resource_internal_id", "DESC");
        if (resources.getTotal() > 0) {
            return resources.getResults().get(0);
        }
        return null;
    }

    @Override
    public ResourceInteroperabilityRecordBundle update(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the ResourceInteroperabilityRecord with id '{}'", auth, resourceInteroperabilityRecordBundle.getId());

        commonMethods.checkRelatedResourceIDsConsistency(resourceInteroperabilityRecordBundle);
        Resource existing = whereID(resourceInteroperabilityRecordBundle.getId(), true);
        ResourceInteroperabilityRecordBundle ex = deserialize(existing);
        // check if there are actual changes in the ResourceInteroperabilityRecord
        if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().equals(ex.getResourceInteroperabilityRecord())){
            throw new ValidationException("There are no changes in the Resource Interoperability Record", HttpStatus.OK);
        }

        // block Public ResourceInteroperabilityRecordBundle updates
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Resource Interoperability Record");
        }

        validate(resourceInteroperabilityRecordBundle);
        checkIfEachInteroperabilityRecordIsApproved(resourceInteroperabilityRecordBundle);

        resourceInteroperabilityRecordBundle.setMetadata(Metadata.updateMetadata(resourceInteroperabilityRecordBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ex, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        resourceInteroperabilityRecordBundle.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        resourceInteroperabilityRecordBundle.setLatestUpdateInfo(loggingInfo);

        existing.setPayload(serialize(resourceInteroperabilityRecordBundle));
        existing.setResourceType(resourceType);

        // block user from updating resourceId
        if (!resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId().equals(ex.getResourceInteroperabilityRecord().getResourceId())
                && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Resource Id with which this ResourceInteroperabilityRecord is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

        return resourceInteroperabilityRecordBundle;
    }

    public void delete(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        // block Public ResourceInteroperabilityRecordBundle deletions
        if (resourceInteroperabilityRecordBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Resource Interoperability Record");
        }
        logger.trace("User is attempting to delete the ResourceInteroperabilityRecord with id '{}'",
                resourceInteroperabilityRecordBundle.getId());
        super.delete(resourceInteroperabilityRecordBundle);
        logger.debug("Deleting ResourceInteroperabilityRecord: {}", resourceInteroperabilityRecordBundle);

    }

    public ResourceInteroperabilityRecordBundle getResourceInteroperabilityRecordByResourceId(String resourceId, String catalogueId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = getAll(ff, auth).getResults();
        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId().equals(catalogueId)
                    && (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId().equals(resourceId))){
                return resourceInteroperabilityRecord;
            }
        }
        return null;
    }

    private ResourceInteroperabilityRecordBundle checkIfEachInteroperabilityRecordIsApproved(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle){
        for (String interoperabilityRecord : resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds()) {
            if (!interoperabilityRecordService.get(interoperabilityRecord).getStatus().equals("approved interoperability record")){
                throw new ValidationException("One ore more of the Interoperability Records you have provided is not yet approved.");
            }
        }
        return resourceInteroperabilityRecordBundle;
    }

    public ResourceInteroperabilityRecordBundle createPublicResourceInteroperabilityRecord(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle, Authentication auth){
        publicResourceInteroperabilityRecordManager.add(resourceInteroperabilityRecordBundle, auth);
        return resourceInteroperabilityRecordBundle;
    }
}
