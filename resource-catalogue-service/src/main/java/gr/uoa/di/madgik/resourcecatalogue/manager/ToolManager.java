package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_FEATURED;
import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_PROVIDERS;
import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;
import static java.util.stream.Collectors.toList;

@org.springframework.stereotype.Service
public class ToolManager extends ResourceManager<ToolBundle> implements ToolService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final CatalogueService catalogueService;
    private final PublicToolManager publicToolManager;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final PublicMonitoringManager publicMonitoringManager;
    private final MigrationService migrationService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericManager genericManager;
    @Autowired
    private FacetLabelService facetLabelService;
    @Autowired
    private FieldValidator fieldValidator;
    @Autowired
    private SearchService searchService;
    @Autowired
    @Qualifier("toolSync")
    private final SynchronizerService<Tool> synchronizerService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ToolManager(ProviderService providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   @Lazy HelpdeskService helpdeskService,
                                   @Lazy MonitoringService monitoringService,
                                   @Lazy ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                   CatalogueService catalogueService,
                                   PublicToolManager publicToolManager,
                                   PublicHelpdeskManager publicHelpdeskManager,
                                   PublicMonitoringManager publicMonitoringManager,
                                   SynchronizerService<Tool> synchronizerService,
                                   ProviderResourcesCommonMethods commonMethods,
                                   GenericManager genericManager,
                                   @Lazy MigrationService migrationService) {
        super(ToolBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.catalogueService = catalogueService;
        this.publicToolManager = publicToolManager;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
        this.synchronizerService = synchronizerService;
        this.commonMethods = commonMethods;
        this.genericManager = genericManager;
        this.migrationService = migrationService;
    }

    @Override
    public String getResourceType() {
        return "tool";
    }

//    @Override
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #toolBundle.payload)")
//    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
//    public ToolBundle add(ToolBundle toolBundle, Authentication auth) {
//        return add(toolBundle, null, auth);
//    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #toolBundle.payload)")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle add(ToolBundle toolBundle, Authentication auth) {
        
        commonMethods.checkRelatedResourceIDsConsistency(toolBundle);
        toolBundle.setId(idCreator.generate(getResourceType()));

        // register and ensure Resource Catalogue's PID uniqueness
        commonMethods.createPIDAndCorrespondingAlternativeIdentifier(toolBundle, getResourceType());
        //toolBundle.getTool().setAlternativeIdentifiers(
        //        commonMethods.ensureResourceCataloguePidUniqueness(toolBundle.getId(),
        //                toolBundle.getTool().getAlternativeIdentifiers()));

        ProviderBundle providerBundle = providerService.get(toolBundle.getTool().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new ValidationException(String.format("Provider with id '%s' ", toolBundle.getTool().getResourceOrganisation()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ValidationException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    toolBundle.getTool().getResourceOrganisation()));
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ValidationException(String.format("The Provider with id %s has already registered a Resource Template.", providerBundle.getId()));
        }
        validateTool(toolBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        toolBundle.setActive(active);

        // create new Metadata if not exists
        if (toolBundle.getMetadata() == null) {
            toolBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(toolBundle, auth);

        // latestOnboardingInfo
        toolBundle.setLatestOnboardingInfo(loggingInfoList.get(0));

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            toolBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            toolBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            toolBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        toolBundle.setLoggingInfo(loggingInfoList);

        logger.info("Adding Tool: {}", toolBundle);
        ToolBundle ret;

        ret = super.add(toolBundle, auth);

        synchronizerService.syncAdd(ret.getTool());

        return ret;
    }

//    @Override
//    public ToolBundle update(ToolBundle toolBundle, String comment, Authentication auth) {
//        return update(toolBundle, toolBundle.getTool().getCatalogueId(), comment, auth);
//    }

    @Override
    public ToolBundle update(ToolBundle toolBundle, String comment, Authentication auth) {

        ToolBundle ret = ObjectUtils.clone(toolBundle);
        ToolBundle existingTool;
        try {
            existingTool = get(ret.getTool().getId());
            if (ret.getTool().equals(existingTool.getTool())) {
                return ret;
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no Tool with id [%s]",
                    ret.getTool().getId()));
        }

        commonMethods.checkRelatedResourceIDsConsistency(ret);

        // ensure Resource Catalogue's PID uniqueness
        //toolBundle.getTool().setAlternativeIdentifiers(
        //        commonMethods.ensureResourceCataloguePidUniqueness(toolBundle.getId(),
        //                toolBundle.getTool().getAlternativeIdentifiers()));

        logger.trace("Attempting to update the Tool with id '{}'", ret.getTool().getId());
        validateTool(ret);

        ProviderBundle providerBundle = providerService.get(ret.getTool().getResourceOrganisation(), auth);

        // block Public Tool update
        if (existingTool.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly update a Public Tool");
        }

        User user = User.of(auth);

        // update existing Tool Metadata, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingTool.getMetadata(), user.getFullName()));
        ret.setMigrationStatus(existingTool.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingTool, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // set active/status
        ret.setActive(existingTool.isActive());
        ret.setStatus(existingTool.getStatus());
        ret.setSuspended(existingTool.isSuspended());

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingTool.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                ret.setStatus(vocabularyService.get("pending resource").getId());
                ret.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }


        logger.info("Updating Tool: {}", ret);
        ret = super.update(ret, auth);

        synchronizerService.syncUpdate(ret.getTool());

        // send notification emails to Portal Admins
        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            Long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            Long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidToolUpdate(ret);
            }
        }

        return ret;
    }

    @Override
    public ToolBundle getCatalogueResource(String catalogueId, String toolId, Authentication auth) {
        ToolBundle toolBundle = get(toolId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (toolBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Tool with id: %s", toolId));
        }
        if (catalogueBundle == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not find Catalogue with id: %s", catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsResourceProviderAdmin(user, toolId, catalogueId)) {
                return toolBundle;
            }
        }
        // else return the Tool ONLY if it is active
        if (toolBundle.getStatus().equals(vocabularyService.get("approved resource").getId())) {
            return toolBundle;
        }
        throw new ValidationException("You cannot view the specific Tool");
    }

    @Override
    public void delete(ToolBundle toolBundle) {
        commonMethods.blockResourceDeletion(toolBundle.getStatus(), toolBundle.getMetadata().isPublished());
        commonMethods.deleteResourceRelatedServiceExtensionsAndResourceInteroperabilityRecords(toolBundle.getId(), catalogueId, "Tool");
        logger.info("Deleting Tool: {}", toolBundle);
        super.delete(toolBundle);
        synchronizerService.syncDelete(toolBundle.getTool());
    }

    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        ToolBundle toolBundle = getCatalogueResource(catalogueId, id, auth);
        toolBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(toolBundle.getTool().getResourceOrganisation(), auth);
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(toolBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                toolBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                toolBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                toolBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                toolBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                toolBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                toolBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }

        logger.info("Verifying Tool: {}", toolBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return update(toolBundle, auth);
    }

    public boolean validateTool(ToolBundle toolBundle) {
        Tool tool = toolBundle.getTool();
        //If we want to reject bad vocab ids instead of silently accept, here's where we do it
        logger.debug("Validating Tool with id: {}", tool.getId());

        try {
            fieldValidator.validate(toolBundle);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
        if (toolBundle.getTool().getScientificDomains() != null &&
                !toolBundle.getTool().getScientificDomains().isEmpty()) {
            validateScientificDomains(toolBundle.getTool().getScientificDomains());
        }

        return true;
    }

    @Override
    public ToolBundle publish(String toolId, Boolean active, Authentication auth) {
        ToolBundle toolBundle;
        String activeProvider = "";
        toolBundle = this.get(toolId, catalogueId);

        if ((toolBundle.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                toolBundle.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !toolBundle.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Tool, because it's Inactive with status = [%s]", toolBundle.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(toolBundle.getTool().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = toolBundle.getTool().getResourceOrganisation();
        }
        if (active && activeProvider.equals("")) {
            throw new ResourceException("Tool does not have active Providers", HttpStatus.CONFLICT);
        }
        toolBundle.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(toolBundle, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        toolBundle.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        toolBundle.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        toolBundle.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        toolBundle.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // active Service's related resources (ServiceExtensions && Subprofiles)
        publishToolRelatedResources(toolBundle.getId(), active, auth);

        update(toolBundle, auth);
        return toolBundle;
    }

    @Override
    public void publishToolRelatedResources(String id, Boolean active, Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id, catalogueId);
        MonitoringBundle monitoringBundle = monitoringService.get(id, catalogueId);
        if (active) {
            logger.info("Activating all related resources of the Tool with id: {}", id);
        } else {
            logger.info("Deactivating all related resources of the Tool with id: {}", id);
        }
        if (helpdeskBundle != null) {
            publishServiceExtensions(helpdeskBundle, active, auth);
        }
        if (monitoringBundle != null) {
            publishServiceExtensions(monitoringBundle, active, auth);
        }
    }

    private void publishServiceExtensions(Bundle<?> bundle, boolean active, Authentication auth) {
        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(bundle, active, auth);

        // update Bundle's fields
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size() - 1));
        bundle.setActive(active);

        if (bundle instanceof HelpdeskBundle) {
            try {
                logger.debug("Setting Helpdesk '{}' of the Tool '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getCatalogueId(), bundle.isActive());
                helpdeskService.updateBundle((HelpdeskBundle) bundle, auth);
                HelpdeskBundle publicHelpdeskBundle =
                        publicHelpdeskManager.getOrElseReturnNull(((HelpdeskBundle) bundle).getCatalogueId() +
                                "." + bundle.getId());
                if (publicHelpdeskBundle != null) {
                    publicHelpdeskManager.update((HelpdeskBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Helpdesk '{}' of the Tool '{}' of the '{}' Catalogue",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getCatalogueId());
            }
        } else {
            try {
                logger.debug("Setting Monitoring '{}' of the Tool '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getCatalogueId(), bundle.isActive());
                monitoringService.updateBundle((MonitoringBundle) bundle, auth);
                MonitoringBundle publicMonitoringBundle =
                        publicMonitoringManager.getOrElseReturnNull(((MonitoringBundle) bundle).getCatalogueId() +
                                "." + bundle.getId());
                if (publicMonitoringBundle != null) {
                    publicMonitoringManager.update((MonitoringBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Monitoring '{}' of the Tool '{}' of the '{}' Catalogue",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getCatalogueId());
            }
        }
    }

    @Override
    public ToolBundle audit(String toolId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ToolBundle tool = get(toolId);
        ProviderBundle provider = providerService.get(tool.getTool().getResourceOrganisation(), auth);
        commonMethods.auditResource(tool, comment, actionType, auth);
//        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
//            tool.setAuditState(Auditable.VALID);
//        }
//        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
//            tool.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
//        }

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(tool, "Tool",
                tool.getTool().getName(), provider.getProvider().getUsers());

        logger.info("User '{}-{}' audited Tool '{}'-'{}' with [actionType: {}]",
                User.of(auth).getFullName(), User.of(auth).getEmail(),
                tool.getTool().getId(), tool.getTool().getName(), actionType);
        return super.update(tool, auth);
    }

    @Override
    public List<ToolBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<ToolBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<Tool> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Tools
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle)) {
                return this.getAll(ff, auth).getResults().stream().map(ToolBundle::getTool).collect(Collectors.toList());
            }
        }
        // else return Provider's Tools ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
            return this.getAll(ff, null).getResults().stream().map(ToolBundle::getTool).collect(Collectors.toList());
        }
        throw new ValidationException("You cannot view the Tools of the specific Provider");
    }

    @Override
    public List<ToolBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, null).getResults();
    }

    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        ToolBundle toolBundle;
        try {
            toolBundle = get(id, catalogueId);
            List<Resource> allResources = getResources(toolBundle.getTool().getId(), catalogueId); // get all versions of a specific Service
            allResources.sort(Comparator.comparing((Resource::getCreationDate)));
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            for (Resource resource : allResources) {
                ToolBundle tool = deserialize(resource);
                if (tool.getLoggingInfo() != null) {
                    loggingInfoList.addAll(tool.getLoggingInfo());
                }
            }
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        } catch (ResourceNotFoundException e) {
            logger.info(String.format("Tool with id [%s] not found", id));
        }
        return null;
    }

    @Override
    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        String providerId = providerService.get(get(resourceId).getTool().getResourceOrganisation()).getId();
        String providerName = providerService.get(get(resourceId).getTool().getResourceOrganisation()).getProvider().getName();
        logger.info(String.format("Mailing provider [%s]-[%s] for outdated Tools", providerId, providerName));
        registrationMailService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId);
    }

    @Override
    public ToolBundle createPublicResource(ToolBundle toolBundle, Authentication auth) {
        publicToolManager.add(toolBundle, auth);
        return toolBundle;
    }

    @Override
    public ToolBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find Tool with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    // Needed for FieldValidation
    @Override
    public ToolBundle get(String id) {
        ToolBundle resource = null;
        try {
            resource = get(id, catalogueId);
        } catch (ResourceNotFoundException e) {
            resource = checkIdExistenceInOtherCatalogues(id);
            if (resource == null) {
                throw e;
            }
        }
        return resource;
    }

    private ToolBundle checkIdExistenceInOtherCatalogues(String id) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("resource_internal_id", id);
        List<ToolBundle> allResources = getAll(ff, null).getResults();
        if (allResources.size() > 0) {
            return allResources.get(0);
        }
        return null;
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Tool> getResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("title", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(ToolBundle::getTool).collect(Collectors.toList());
    }

    public List<Resource> getResources(String id, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\"", id),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources != null) {
            return resources.getResults();
        }
        return Collections.emptyList();
    }

    @Override
    public ToolBundle getOrElseReturnNull(String id) {
        ToolBundle toolBundle;
        try {
            toolBundle = get(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return toolBundle;
    }

    @Override
    public ToolBundle getOrElseReturnNull(String id, String catalogueId) {
        ToolBundle toolBundle;
        try {
            toolBundle = get(id, catalogueId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
        return toolBundle;
    }

    @Override
    public List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec) {
        List<String> finalResults = new ArrayList<>();
        List<String> allSub = new ArrayList<>();
        List<String> correctedSubs = new ArrayList<>();
        for (Map<String, Object> map : rec) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String trimmed = entry.getValue().toString().replace("{", "").replace("}", "");
                if (!allSub.contains(trimmed)) {
                    allSub.add((trimmed));
                }
            }
        }
        // Required step to fix joint subcategories (sub1,sub2,sub3) who passed as 1 value
        for (String item : allSub) {
            if (item.contains(",")) {
                String[] itemParts = item.split(",");
                correctedSubs.addAll(Arrays.asList(itemParts));
            } else {
                correctedSubs.add(item);
            }
        }
        if (type.equalsIgnoreCase("SCIENTIFIC_DOMAIN")) {
            String[] parts = parent.split("-");
            for (String id : correctedSubs) {
                if (id.contains(parts[1])) {
                    finalResults.add(id);
                }
            }
        } else {
            String[] parts = parent.split("-");
            for (String id : correctedSubs) {
                if (id.contains(parts[2])) {
                    finalResults.add(id);
                }
            }
        }
        return finalResults;
    }

    @Override
    public Browsing<ToolBundle> getAll(FacetFilter ff, Authentication auth) {
        // if user is Unauthorized, return active/latest ONLY
        if (auth == null) {
            ff.addFilter("active", true);
            ff.addFilter("published", false);
        }
        if (auth != null && auth.isAuthenticated()) {
            // if user is Authorized with ROLE_USER, return active/latest ONLY
            if (!securityService.hasRole(auth, "ROLE_PROVIDER") && !securityService.hasRole(auth, "ROLE_EPOT") &&
                    !securityService.hasRole(auth, "ROLE_ADMIN")) {
                ff.addFilter("active", true);
                ff.addFilter("published", false);
            }
        }

        ff.setBrowseBy(genericManager.getBrowseBy(getResourceType()));
        ff.setResourceType(getResourceType());

        return getMatchingResources(ff);
    }

    @Override
    public Browsing<ToolBundle> getAllForAdmin(FacetFilter filter, Authentication auth) {
        filter.setBrowseBy(genericManager.getBrowseBy(getResourceType()));
        filter.setResourceType(getResourceType());
        return getMatchingResources(filter);
    }

    private Browsing<ToolBundle> getMatchingResources(FacetFilter ff) {
        Browsing<ToolBundle> resources;

        resources = getResults(ff);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.generateLabels(resources.getFacets()));
        }

        return resources;
    }

    @Override
    protected Browsing<ToolBundle> getResults(FacetFilter filter) {
        Browsing<ToolBundle> browsing;
        filter.setResourceType(getResourceType());
        browsing = convertToBrowsingEIC(searchService.search(filter));

        return browsing;
    }

    private Browsing<ToolBundle> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<ToolBundle> results = paging.getResults()
                .stream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, genericManager.getLabels(getResourceType()));
    }

    @Override
    public Paging<ToolBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved resource");
        facetFilter.addFilter("published", false);
        Browsing<ToolBundle> toolBrowsing = getAll(facetFilter, auth);
        List<ToolBundle> toolsToBeAudited = new ArrayList<>();
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (ToolBundle toolBundle : toolBrowsing.getResults()) {
            if (toolBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(toolBundle.getLatestAuditInfo().getDate()) > interval) {
                    toolsToBeAudited.add(toolBundle);
                }
            }
        }
        Collections.shuffle(toolsToBeAudited);
        for (int i = toolsToBeAudited.size() - 1; i > ff.getQuantity() - 1; i--) {
            toolsToBeAudited.remove(i);
        }
        return new Browsing<>(toolsToBeAudited.size(), 0, toolsToBeAudited.size(), toolsToBeAudited, toolBrowsing.getFacets());
    }

    @Override
    public ToolBundle getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        List<ToolBundle> allProviderTools = getAll(ff, auth).getResults();
        for (ToolBundle toolBundle : allProviderTools) {
            if (toolBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return toolBundle;
            }
        }
        return null;
    }

    @Override
    public Map<String, List<ToolBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    public List<Tool> getByIds(Authentication auth, String... ids) {
        List<Tool> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId).getTool();
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
    }

    @Override
    public ToolBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        ToolBundle toolBundle = get(resourceId, catalogueId);
        // check Datasource's status
        if (!toolBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Tool with id [%s] to another Provider as it" +
                    "is not yet Approved", toolBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(toolBundle.getTool().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot move a Tool to a Provider of another Catalogue");
        }

        User user = User.of(auth);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = toolBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || "".equals(comment)) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        toolBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        toolBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = toolBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(user.getFullName());
        metadata.setTerms(null);
        toolBundle.setMetadata(metadata);

        // update ResourceOrganisation
        toolBundle.getTool().setResourceOrganisation(newProviderId);

        // update ResourceProviders
        List<String> resourceProviders = toolBundle.getTool().getResourceProviders();
        if (resourceProviders.contains(oldProvider.getId())) {
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // add Resource, delete the old one
        add(toolBundle, auth);
        publicToolManager.delete(get(resourceId, catalogueId)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueId));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, toolBundle.getId());

        // emails to EPOT, old and new Provider
        //registrationMailService.sendEmailsForMovedTools(oldProvider, newProvider, toolBundle, auth);

        return toolBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public ToolBundle suspend(String toolId, boolean suspend, Authentication auth) {
        ToolBundle toolBundle = get(toolId);
        commonMethods.suspensionValidation(toolBundle,
                toolBundle.getTool().getResourceOrganisation(), suspend, auth);
        commonMethods.suspendResource(toolBundle, suspend, auth);
        // suspend Service's extensions
        HelpdeskBundle helpdeskBundle = helpdeskService.get(toolId, null);
        if (helpdeskBundle != null) {
            try {
                commonMethods.suspendResource(helpdeskBundle, suspend, auth);
                helpdeskService.update(helpdeskBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        MonitoringBundle monitoringBundle = monitoringService.get(toolId, null);
        if (monitoringBundle != null) {
            try {
                commonMethods.suspendResource(monitoringBundle, suspend, auth);
                monitoringService.update(monitoringBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend ResourceInteroperabilityRecord
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(toolId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                commonMethods.suspendResource(resourceInteroperabilityRecordBundle, suspend, auth);
                resourceInteroperabilityRecordService.update(resourceInteroperabilityRecordBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return super.update(toolBundle, auth);
    }

}
