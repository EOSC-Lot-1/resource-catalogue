package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.*;

@Service("draftToolManager")
public class DraftToolManager extends ResourceManager<ToolBundle> implements DraftResourceService<ToolBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftServiceManager.class);

    private final ToolService toolService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;

    public DraftToolManager(ToolService toolService,
                                        IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                                        @Lazy ProviderService providerService,
                                        ProviderResourcesCommonMethods commonMethods) {
        super(ToolBundle.class);
        this.toolService = toolService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "draft_tool";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle add(ToolBundle bundle, Authentication auth) {

        bundle.setId(idCreator.generate(getResourceType()));

        logger.trace("Attempting to add a new Draft Tool with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setActive(false);
        bundle.setDraft(true);

        super.add(bundle, auth);

        return bundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle update(ToolBundle bundle, Authentication auth) {
        // get existing resource
        Resource existing = getDraftResource(bundle.getTool().getId());
        // block catalogueId updates from Provider Admins
        //bundle.getTool().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Tool with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(bundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Tool: {}", bundle);
        return bundle;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(ToolBundle bundle) {
        super.delete(bundle);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle transformToNonDraft(String id, Authentication auth) {
        ToolBundle toolBundle = this.get(id);
        return transformToNonDraft(toolBundle, auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle transformToNonDraft(ToolBundle bundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Tool with id {} to Tool", bundle.getId());
        toolService.validate(bundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerService.get(bundle.getTool().getResourceOrganisation()).getTemplateStatus().equals("approved template")) {
            bundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);
            bundle.setActive(true);
        } else {
            bundle.setStatus(vocabularyService.get("pending resource").getId());
        }
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        bundle.setDraft(false);

        ResourceType toolType = resourceTypeService.getResourceType("tool");
        Resource resource = getDraftResource(bundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, toolType);

        try {
            bundle = toolService.update(bundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return bundle;
    }

    public List<ToolBundle> getMy(Authentication auth) {
        //TODO: Implement
        List<ToolBundle> re = new ArrayList<>();
        return re;
    }

    private Resource getDraftResource(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" ", id),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

}
