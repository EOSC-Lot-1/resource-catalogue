package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.dto.MapValues;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.service.SynchronizerService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.validators.FieldValidator;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;

import javax.sql.DataSource;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.einfracentral.config.CacheConfig.*;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateMerilScientificDomains;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;

@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends ResourceManager<ProviderBundle> implements ProviderService<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService;
    private final PublicServiceManager publicServiceManager;
    private final PublicProviderManager publicProviderManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final PublicInteroperabilityRecordManager publicInteroperabilityRecordManager;
    private final SecurityService securityService;
    private final FieldValidator fieldValidator;
    private final IdCreator idCreator;
    private final EventService eventService;
    private final RegistrationMailService registrationMailService;
    private final VersionService versionService;
    private final VocabularyService vocabularyService;
    private final DataSource dataSource;
    private final CatalogueService<CatalogueBundle, Authentication> catalogueService;
    private final SynchronizerService<Provider> synchronizerService;
    private final ProviderResourcesCommonMethods commonMethods;
    @Autowired
    CacheManager cacheManager;

    //TODO: maybe add description on DB and elastic too
    private final String columnsOfInterest = "provider_id, name"; // variable with DB tables a keyword is been searched on

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public ProviderManager(@Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                           @Lazy SecurityService securityService, @Lazy FieldValidator fieldValidator,
                           @Lazy RegistrationMailService registrationMailService, IdCreator idCreator,
                           EventService eventService, VersionService versionService,
                           VocabularyService vocabularyService, DataSource dataSource,
                           @Qualifier("providerSync") SynchronizerService<Provider> synchronizerService,
                           ProviderResourcesCommonMethods commonMethods,
                           CatalogueService<CatalogueBundle, Authentication> catalogueService,
                           @Lazy PublicServiceManager publicServiceManager,
                           @Lazy PublicProviderManager publicProviderManager,
                           @Lazy TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                           @Lazy InteroperabilityRecordService<InteroperabilityRecordBundle> interoperabilityRecordService,
                           @Lazy PublicTrainingResourceManager publicTrainingResourceManager,
                           @Lazy PublicInteroperabilityRecordManager publicInteroperabilityRecordManager) {
        super(ProviderBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.securityService = securityService;
        this.fieldValidator = fieldValidator;
        this.idCreator = idCreator;
        this.eventService = eventService;
        this.registrationMailService = registrationMailService;
        this.versionService = versionService;
        this.vocabularyService = vocabularyService;
        this.dataSource = dataSource;
        this.synchronizerService = synchronizerService;
        this.commonMethods = commonMethods;
        this.catalogueService = catalogueService;
        this.publicServiceManager = publicServiceManager;
        this.publicProviderManager = publicProviderManager;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.publicInteroperabilityRecordManager = publicInteroperabilityRecordManager;
    }


    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public boolean exists(ProviderBundle providerBundle) {
        return getResource(providerBundle.getProvider().getId(), providerBundle.getProvider().getCatalogueId()) != null;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle provider, Authentication authentication) {
        return add(provider, null, authentication);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle add(ProviderBundle provider, String catalogueId, Authentication auth) {
        logger.trace("User '{}' is attempting to add a new Provider: {} on Catalogue: {}", auth, provider, catalogueId);

        provider = onboard(provider, catalogueId, auth);

        // prohibit EOSC related Alternative Identifier Types
        commonMethods.prohibitEOSCRelatedPIDs(provider.getProvider().getAlternativeIdentifiers());

        provider.setId(idCreator.createProviderId(provider.getProvider()));
        addAuthenticatedUser(provider.getProvider(), auth);
        validate(provider);
        provider.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));

        ProviderBundle ret;
        ret = super.add(provider, null);
        logger.debug("Adding Provider: {} of Catalogue: {}", provider, catalogueId);

        registrationMailService.sendEmailsToNewlyAddedAdmins(provider, null);

        synchronizerService.syncAdd(provider.getProvider());

        return ret;
    }

    //    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle provider, String comment, Authentication auth) {
        return update(provider, provider.getProvider().getCatalogueId(), comment, auth);
    }

    //    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle update(ProviderBundle providerBundle, String catalogueId, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Provider with id '{}' of the Catalogue '{}'", auth, providerBundle, providerBundle.getProvider().getCatalogueId());

        ProviderBundle ret = ObjectUtils.clone(providerBundle);
        Resource existingResource = getResource(ret.getId(), ret.getProvider().getCatalogueId());
        ProviderBundle existingProvider = deserialize(existingResource);
        // check if there are actual changes in the Provider
        if (ret.getTemplateStatus().equals(existingProvider.getTemplateStatus()) && ret.getProvider().equals(existingProvider.getProvider())){
            if (ret.isSuspended() == existingProvider.isSuspended()){
                return ret;
            }
        }

        if (catalogueId == null || catalogueId.equals("")) {
            ret.getProvider().setCatalogueId(catalogueName);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }

        // prohibit EOSC related Alternative Identifier Types
        commonMethods.prohibitEOSCRelatedPIDs(ret.getProvider().getAlternativeIdentifiers());

        // block Public Provider update
        if (ret.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly update a Public Provider");
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN") && !existingProvider.getProvider().getCatalogueId().equals(ret.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot change catalogueId");
        }
        ret.setActive(existingProvider.isActive());
        ret.setStatus(existingProvider.getStatus());
        ret.setSuspended(existingProvider.isSuspended());
        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);
        resourceService.updateResource(existingResource);
        logger.debug("Updating Provider: {} of Catalogue: {}", ret, ret.getProvider().getCatalogueId());

        // check if Provider has become a Legal Entity
        checkAndAddProviderToHLEVocabulary(ret);

        // Send emails to newly added or deleted Admins
        adminDifferences(ret, existingProvider);

        // send notification emails to Portal Admins
        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidProviderUpdate(ret);
            }
        }

        synchronizerService.syncUpdate(ret.getProvider());

        return ret;
    }

    /**
     * Do not expose this method to users because it returns sensitive information about providers.
     *
     * @param id
     * @return
     */
    private ProviderBundle getWithCatalogue(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(String.format("Could not find provider with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

    @Cacheable(value = CACHE_PROVIDERS, key = "#catalogueId+#providerId+(#auth!=null?#auth:'')")
    public ProviderBundle get(String catalogueId, String providerId, Authentication auth) {
        ProviderBundle providerBundle = getWithCatalogue(providerId, catalogueId);
        CatalogueBundle catalogueBundle = catalogueService.get(catalogueId);
        if (providerBundle == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find provider with id: %s", providerId));
        }
        if (catalogueBundle == null) {
            throw new eu.einfracentral.exception.ResourceNotFoundException(
                    String.format("Could not find catalogue with id: %s", catalogueId));
        }
        if (!providerBundle.getProvider().getCatalogueId().equals(catalogueId)){
            throw new ValidationException(String.format("Provider with id [%s] does not belong to the catalogue with id [%s]", providerId, catalogueId));
        }
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle)) {
                return providerBundle;
            }
        }
        // else return the Provider ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return providerBundle;
        }
        throw new ValidationException("You cannot view the specific Provider");
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key = "#id+(#auth!=null?#auth:'')")
    public ProviderBundle get(String id, Authentication auth) {
        ProviderBundle providerBundle = get(id);
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return everything
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userIsProviderAdmin(user, providerBundle)) {
                return providerBundle;
            }
        }
        // else return the Provider ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())){
            return providerBundle;
        }
        throw new ValidationException("You cannot view the specific Provider");
    }

    @Override
    public Paging<ResourceHistory> getHistory(String id, String catalogueId) {
        Map<String, ResourceHistory> historyMap = new TreeMap<>();

        Resource resource = getResource(id, catalogueId);
        List<Version> versions = versionService.getVersionsByResource(resource.getId());
        versions.sort((version, t1) -> {
            if (version.getCreationDate().getTime() < t1.getCreationDate().getTime()) {
                return -1;
            }
            return 1;
        });

        // create the first entry from the current resource
        ProviderBundle providerBundle;
        providerBundle = deserialize(resource);
        if (providerBundle != null && providerBundle.getMetadata() != null) {
            historyMap.put(providerBundle.getMetadata().getModifiedAt(), new ResourceHistory(providerBundle, resource.getId()));
        }

        // create version entries
        for (Version version : versions) {
            resource = (version.getResource() == null ? getResource(version.getParentId()) : version.getResource());
            resource.setPayload(version.getPayload());
            providerBundle = deserialize(resource);
            if (providerBundle != null) {
                try {
                    historyMap.putIfAbsent(providerBundle.getMetadata().getModifiedAt(), new ResourceHistory(providerBundle, version.getId()));
                } catch (NullPointerException e) {
                    logger.warn("Provider with id '{}' does not have Metadata", providerBundle.getId());
                }
            }
        }

        // sort list by modification date
        List<ResourceHistory> history = new ArrayList<>(historyMap.values());
        history.sort((resourceHistory, t1) -> {
            if (Long.parseLong(resourceHistory.getModifiedAt()) < Long.parseLong(t1.getModifiedAt())) {
                return 1;
            }
            return -1;
        });

        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key="#ff.hashCode()+(#auth!=null?#auth.hashCode():0)")
    public Browsing<ProviderBundle> getAll(FacetFilter ff, Authentication auth) {
        List<ProviderBundle> retList = new ArrayList<>();

        // if user is ADMIN or EPOT return everything
        if (auth != null && auth.isAuthenticated()) {
            if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                    securityService.hasRole(auth, "ROLE_EPOT")) {
                return super.getAll(ff, auth);
            }
            // if user is PROVIDER ADMIN return all his Providers (rejected, pending) with their sensitive data (Users, MainContact) too
            User user = User.of(auth);
            Browsing<ProviderBundle> providers = super.getAll(ff, auth);
            for (ProviderBundle providerBundle : providers.getResults()){
                if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId()) ||
                        securityService.userIsProviderAdmin(user, providerBundle)) {
                    retList.add(providerBundle);
                }
            }
            providers.setResults(retList);
            providers.setTotal(retList.size());
            providers.setTo(retList.size());
            return providers;
        }

        // else return ONLY approved Providers
        ff.addFilter("status", "approved provider");
        Browsing<ProviderBundle> providers = super.getAll(ff, auth);
        retList.addAll(providers.getResults());
        providers.setResults(retList);

        return providers;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void delete(ProviderBundle provider) {
        String catalogueId = provider.getProvider().getCatalogueId();
        // block Public Provider update
        if (provider.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Provider");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.trace("User is attempting to delete the Provider with id '{}'", provider.getId());
        List<ServiceBundle> services = serviceBundleService.getResourceBundles(catalogueId, provider.getId(), authentication).getResults();
        services.forEach(s -> {
            if (!s.getMetadata().isPublished()){
                try {
                    serviceBundleService.delete(s);
                } catch (ResourceNotFoundException e) {
                    logger.error(String.format("Error deleting Service with ID [%s]", s.getId()));
                }
            }
        });
        List<TrainingResourceBundle> trainingResources = trainingResourceService.getResourceBundles(catalogueId, provider.getId(), authentication).getResults();
        trainingResources.forEach(s -> {
            if (!s.getMetadata().isPublished()){
                try {
                    trainingResourceService.delete(s);
                } catch (ResourceNotFoundException e) {
                    logger.error(String.format("Error deleting Training Resource with ID [%s]", s.getId()));
                }
            }
        });
        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, provider.getId(), authentication).getResults();
        interoperabilityRecords.forEach(s -> {
            if (!s.getMetadata().isPublished()){
                try {
                    interoperabilityRecordService.delete(s);
                } catch (ResourceNotFoundException e) {
                    logger.error(String.format("Error deleting Interoperability Record with ID [%s]", s.getId()));
                }
            }
        });
        logger.debug("Deleting Provider: {} and all his Resources", provider);

        deleteBundle(provider);
        logger.debug("Deleting Resource {}", provider);

        // TODO: move to aspect
        registrationMailService.notifyProviderAdmins(provider);

        synchronizerService.syncDelete(provider.getProvider());

    }

    private void deleteBundle(ProviderBundle providerBundle) {
        // block Public Provider update
        if (providerBundle.getMetadata().isPublished()){
            throw new ValidationException("You cannot directly delete a Public Provider");
        }
        logger.info("Deleting Provider: {}", providerBundle);
        super.delete(providerBundle);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle verifyProvider(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Provider state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Provider State!", status));
        }
        logger.trace("verifyProvider with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        ProviderBundle provider = get(catalogueName, id, auth);
        provider.setStatus(vocabularyService.get(status).getId());
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);
        LoggingInfo loggingInfo = null;

        switch (status) {
            case "approved provider":
                if (active == null) {
                    active = true;
                }
                provider.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());

                // add Provider's Name as a HLE Vocabulary
                checkAndAddProviderToHLEVocabulary(provider);
                break;
            case "rejected provider":
                provider.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                break;
            default:
                break;
        }
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        provider.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        provider.setLatestOnboardingInfo(loggingInfo);

        logger.info("Verifying Provider: {}", provider);
        return super.update(provider, auth);
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle publish(String providerId, Boolean active, Authentication auth) {
        ProviderBundle provider = getWithCatalogue(providerId, catalogueName);
        if ((provider.getStatus().equals(vocabularyService.get("pending provider").getId()) ||
                provider.getStatus().equals(vocabularyService.get("rejected provider").getId())) && !provider.isActive()){
            throw new ValidationException(String.format("You cannot activate this Provider, because it's Inactive with status = [%s]", provider.getStatus()));
        }
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);
        LoggingInfo loggingInfo;

        if (active == null) {
            active = false;
        }
        provider.setActive(active);
        if (active) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.ACTIVATED.getKey());
            logger.info("Activating Provider: {}", provider);
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.DEACTIVATED.getKey());
            logger.info("Deactivating Provider: {}", provider);
        }
        activateProviderResources(provider.getId(), active, auth);
        loggingInfoList.add(loggingInfo);
        provider.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        provider.setLatestUpdateInfo(loggingInfo);
        provider.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        provider.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        return super.update(provider, auth);
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key = "#email+(#auth!=null?#auth:'')")
    public List<ProviderBundle> getServiceProviders(String email, Authentication auth) {
        List<ProviderBundle> providers;
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        } else if (securityService.hasRole(auth, "ROLE_ADMIN") ||
                securityService.hasRole(auth, "ROLE_EPOT")) {
            FacetFilter ff = new FacetFilter();
            ff.setQuantity(maxQuantity);
            ff.addFilter("published", false);
            providers = super.getAll(ff, null).getResults();
        } else if (securityService.hasRole(auth, "ROLE_PROVIDER")) {
            providers = getMy(null, auth).getResults();
        } else {
            return new ArrayList<>();
        }
        return providers
                .stream()
                .map(p -> {
                    if (p.getProvider().getUsers() != null && p.getProvider().getUsers().stream().filter(Objects::nonNull).anyMatch(u -> {
                        if (u.getEmail() != null) {
                            return u.getEmail().equalsIgnoreCase(email);
                        }
                        return false;
                    })) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = CACHE_PROVIDERS, key = "(#auth!=null?#auth:'')")
    public Browsing<ProviderBundle> getMy(FacetFilter ff, Authentication auth) {
        if (auth == null) {
            throw new UnauthorizedUserException("Please log in.");
        }
        User user = User.of(auth);
        if (ff == null) {
            ff = new FacetFilter();
            ff.setQuantity(maxQuantity);
        }
        if (!ff.getFilter().containsKey("published")) {
            ff.addFilter("published", false);
        }
        ff.addFilter("users", user.getEmail());
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth);
    }

    @Override
    public List<ProviderBundle> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.addFilter("published", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return getAll(ff, null).getResults();
    }

    public void activateProviderResources(String providerId, Boolean active, Authentication auth) {
        List<ServiceBundle> services = serviceBundleService.getResourceBundles(providerId, auth);
        List<TrainingResourceBundle> trainingResources = trainingResourceService.getResourceBundles(providerId, auth);
        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueName, providerId, auth).getResults();
        if (active){
            logger.info("Activating all Resources of the Provider with id: {}", providerId);
        } else{
            logger.info("Deactivating all Resources of the Provider with id: {}", providerId);
        }
        activateProviderServices(services, active, auth);
        activateProviderTrainingResources(trainingResources, active, auth);
        activateProviderInteroperabilityRecords(interoperabilityRecords, active, auth);
    }

    private void activateProviderServices(List<ServiceBundle> services, Boolean active, Authentication auth){
        for (ServiceBundle service : services){
            if (service.getStatus().equals("approved resource")) {
                ServiceBundle lowerLevelService = ObjectUtils.clone(service);
                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(service, active, auth);

                // update Service's fields
                service.setLoggingInfo(loggingInfoList);
                service.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size()-1));
                service.setActive(active);

                try {
                    logger.debug("Setting Service '{}'-'{}' of the '{}' Catalogue to active: '{}'", service.getId(),
                            service.getService().getName(), service.getService().getCatalogueId(), service.isActive());
                    serviceBundleService.update(service, auth);
                    // TODO: FIX ON ProviderManagementAspect
                    publicServiceManager.update(service, auth);
                } catch (ResourceNotFoundException e) {
                    logger.error("Could not update Service '{}'-'{}' of the '{}' Catalogue", service.getId(),
                            service.getService().getName(), service.getService().getCatalogueId());
                }

                // Activate/Deactivate Service's Extensions && Subprofiles
                serviceBundleService.publishServiceRelatedResources(lowerLevelService.getId(),
                        lowerLevelService.getService().getCatalogueId(), active, auth);
            }
        }
    }

    private void activateProviderTrainingResources(List<TrainingResourceBundle> trainingResources, Boolean active, Authentication auth){
        for (TrainingResourceBundle trainingResourceBundle : trainingResources){
            if (trainingResourceBundle.getStatus().equals("approved resource")) {
                TrainingResourceBundle lowerLevelTrainingResource = ObjectUtils.clone(trainingResourceBundle);
                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(trainingResourceBundle, active, auth);

                // update Service's fields
                trainingResourceBundle.setLoggingInfo(loggingInfoList);
                trainingResourceBundle.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size()-1));
                trainingResourceBundle.setActive(active);

                try {
                    logger.debug("Setting Training Resource '{}'-'{}' of the '{}' Catalogue to active: '{}'", trainingResourceBundle.getId(),
                            trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getCatalogueId(),
                            trainingResourceBundle.isActive());
                    trainingResourceService.update(trainingResourceBundle, auth);
                    // TODO: FIX ON ProviderManagementAspect
                    publicTrainingResourceManager.update(trainingResourceBundle, auth);
                } catch (ResourceNotFoundException e) {
                    logger.error("Could not update Training Resource '{}'-'{}' of the '{}' Catalogue", trainingResourceBundle.getId(),
                            trainingResourceBundle.getTrainingResource().getTitle(), trainingResourceBundle.getTrainingResource().getCatalogueId());
                }

                // Activate/Deactivate Training Resource's Extensions
                trainingResourceService.publishTrainingResourceRelatedResources(lowerLevelTrainingResource.getId(),
                        lowerLevelTrainingResource.getTrainingResource().getCatalogueId(), active, auth);
            }
        }
    }

    private void activateProviderInteroperabilityRecords(List<InteroperabilityRecordBundle> interoperabilityRecords, Boolean active, Authentication auth){
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecords) {
            if (interoperabilityRecordBundle.getStatus().equals("approved interoperability record")) {
                List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(interoperabilityRecordBundle, active, auth);

                // update Service's fields
                interoperabilityRecordBundle.setLoggingInfo(loggingInfoList);
                interoperabilityRecordBundle.setLatestUpdateInfo(loggingInfoList.get(loggingInfoList.size()-1));
                interoperabilityRecordBundle.setActive(active);

                try {
                    logger.debug("Setting Interoperability Record '{}'-'{}' of the '{}' Catalogue to active: '{}'", interoperabilityRecordBundle.getId(),
                            interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                            interoperabilityRecordBundle.isActive());
                    interoperabilityRecordService.update(interoperabilityRecordBundle, auth);
                    // TODO: FIX ON ProviderManagementAspect
                    publicInteroperabilityRecordManager.update(interoperabilityRecordBundle, auth);
                } catch (ResourceNotFoundException e) {
                    logger.error("Could not update Interoperability Record '{}'-'{}' of the '{}' Catalogue", interoperabilityRecordBundle.getId(),
                            interoperabilityRecordBundle.getInteroperabilityRecord().getTitle(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());
                }
            }
        }
    }

    @Override
    public ProviderBundle validate(ProviderBundle provider) {
        logger.debug("Validating Provider with id: {}", provider.getId());

        try {
            fieldValidator.validate(provider);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        if (provider.getProvider().getScientificDomains() != null && !provider.getProvider().getScientificDomains().isEmpty()) {
            validateScientificDomains(provider.getProvider().getScientificDomains());
        }
        if (provider.getProvider().getMerilScientificDomains() != null && !provider.getProvider().getMerilScientificDomains().isEmpty()) {
            validateMerilScientificDomains(provider.getProvider().getMerilScientificDomains());
        }

        return provider;
    }

    @Override
    @CacheEvict(value = {CACHE_PROVIDERS, CACHE_SERVICE_EVENTS, CACHE_EVENTS}, allEntries = true)
    public void deleteUserInfo(Authentication authentication) {
        logger.trace("User '{}' is attempting to delete his User Info", authentication);
        String userEmail = ((OIDCAuthenticationToken) authentication).getUserInfo().getEmail();
        String userId = ((OIDCAuthenticationToken) authentication).getUserInfo().getSub();
        List<Event> allUserEvents = new ArrayList<>();
        allUserEvents.addAll(eventService.getUserEvents(Event.UserActionType.FAVOURITE.getKey(), authentication));
        allUserEvents.addAll(eventService.getUserEvents(Event.UserActionType.RATING.getKey(), authentication));
        List<ProviderBundle> allUserProviders = getMy(null, authentication).getResults();
        for (ProviderBundle providerBundle : allUserProviders) {
            if (providerBundle.getProvider().getUsers().size() == 1) {
                throw new ValidationException(String.format("Your user info cannot be deleted, because you are the solely Admin of the Provider [%s]. " +
                        "You need to delete your Provider first or add more Admins.", providerBundle.getProvider().getName()));
            }
        }
        logger.info("Attempting to delete all user events");
        eventService.deleteEvents(allUserEvents);
        for (ProviderBundle providerBundle : allUserProviders) {
            List<User> updatedUsers = new ArrayList<>();
            for (User user : providerBundle.getProvider().getUsers()) {
                if (user.getId() != null && !"".equals(user.getId())) {
                    if (!user.getId().equals(userId)) {
                        updatedUsers.add(user);
                    }
                } else {
                    if (!user.getEmail().equals("") && !user.getEmail().equalsIgnoreCase(userEmail)) {
                        updatedUsers.add(user);
                    }
                }
            }
            providerBundle.getProvider().setUsers(updatedUsers);
            update(providerBundle, authentication);
        }
    }

    private void addAuthenticatedUser(Provider provider, Authentication auth) {
        List<User> users;
        User authUser = User.of(auth);
        users = provider.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
    }

    // For front-end use
    public boolean validateUrl(URL urlForValidation) {
        try {
            fieldValidator.validateUrl(null, urlForValidation);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth) {
        ProviderBundle providerBundle = get(catalogueName, providerId, auth);
        List<String> userList = new ArrayList<>();
        for (User user : providerBundle.getProvider().getUsers()) {
            userList.add(user.getEmail().toLowerCase());
        }
        if ((providerBundle.getMetadata().getTerms() == null || providerBundle.getMetadata().getTerms().isEmpty())) {
            if (userList.contains(User.of(auth).getEmail().toLowerCase())) {
                return false; //pop-up modal
            } else {
                return true; //no modal
            }
        }
        if (!providerBundle.getMetadata().getTerms().contains(User.of(auth).getEmail().toLowerCase()) && userList.contains(User.of(auth).getEmail().toLowerCase())) {
            return false; // pop-up modal
        }
        return true; // no modal
    }

    public void adminAcceptedTerms(String providerId, Authentication auth) {
        update(get(providerId), catalogueName, auth);
    }

    public void adminDifferences(ProviderBundle updatedProvider, ProviderBundle existingProvider) {
        List<String> existingAdmins = new ArrayList<>();
        List<String> newAdmins = new ArrayList<>();
        for (User user : existingProvider.getProvider().getUsers()) {
            existingAdmins.add(user.getEmail().toLowerCase());
        }
        for (User user : updatedProvider.getProvider().getUsers()) {
            newAdmins.add(user.getEmail().toLowerCase());
        }
        List<String> adminsAdded = new ArrayList<>(newAdmins);
        adminsAdded.removeAll(existingAdmins);
        if (!adminsAdded.isEmpty()) {
            registrationMailService.sendEmailsToNewlyAddedAdmins(updatedProvider, adminsAdded);
        }
        List<String> adminsDeleted = new ArrayList<>(existingAdmins);
        adminsDeleted.removeAll(newAdmins);
        if (!adminsDeleted.isEmpty()) {
            registrationMailService.sendEmailsToNewlyDeletedAdmins(existingProvider, adminsDeleted);
        }
    }

    public void requestProviderDeletion(String providerId, Authentication auth) {
        ProviderBundle provider = getWithCatalogue(providerId, catalogueName);
        for (User user : provider.getProvider().getUsers()) {
            if (user.getEmail().equalsIgnoreCase(User.of(auth).getEmail())) {
                registrationMailService.informPortalAdminsForProviderDeletion(provider, User.of(auth));
            }
        }
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle auditProvider(String providerId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ProviderBundle provider = getWithCatalogue(providerId, catalogueId);
        commonMethods.auditResource(provider, comment, actionType, auth);

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(provider, "Provider",
                provider.getProvider().getName(), provider.getProvider().getUsers());

        logger.info(String.format("Auditing Provider [%s]-[%s]", catalogueId, provider));
        return super.update(provider, auth);
    }

    public Paging<ProviderBundle> getRandomProviders(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved provider");
        facetFilter.addFilter("published", false);
        Browsing<ProviderBundle> providerBrowsing = getAll(facetFilter, auth);
        List<ProviderBundle> providersToBeAudited = new ArrayList<>();
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (ProviderBundle providerBundle : providerBrowsing.getResults()) {
            if (providerBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(providerBundle.getLatestAuditInfo().getDate()) > interval) {
                    providersToBeAudited.add(providerBundle);
                }
            }
        }
        Collections.shuffle(providersToBeAudited);
        for (int i = providersToBeAudited.size() - 1; i > ff.getQuantity() - 1; i--) {
            providersToBeAudited.remove(i);
        }
        return new Browsing<>(providersToBeAudited.size(), 0, providersToBeAudited.size(), providersToBeAudited, providerBrowsing.getFacets());
    }

    //    @Override
    public Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId) {
        ProviderBundle providerBundle = getWithCatalogue(id, catalogueId);
        if (providerBundle.getLoggingInfo() != null){
            List<LoggingInfo> loggingInfoList = providerBundle.getLoggingInfo();
            loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
            return new Browsing<>(loggingInfoList.size(), 0, loggingInfoList.size(), loggingInfoList, null);
        }
        return null;
    }

    public Paging<ProviderBundle> determineAuditState(Set<String> auditState, FacetFilter ff, List<ProviderBundle> providers, Authentication auth) {
        List<ProviderBundle> valid = new ArrayList<>();
        List<ProviderBundle> notAudited = new ArrayList<>();
        List<ProviderBundle> invalidAndUpdated = new ArrayList<>();
        List<ProviderBundle> invalidAndNotUpdated = new ArrayList<>();

        Paging<ProviderBundle> retPaging = getAll(ff, auth);
        List<ProviderBundle> allWithoutAuditFilterList = new ArrayList<>();
        if (providers.isEmpty()){
            allWithoutAuditFilterList = getAll(ff, auth).getResults();
        } else{
            allWithoutAuditFilterList.addAll(providers);
        }
        List<ProviderBundle> ret = new ArrayList<>();
        for (ProviderBundle providerBundle : allWithoutAuditFilterList){
            String auditVocStatus;
            try{
                auditVocStatus = LoggingInfo.createAuditVocabularyStatuses(providerBundle.getLoggingInfo());
            } catch (NullPointerException e){ // providerBundle has null loggingInfo
                continue;
            }
            switch (auditVocStatus){
                case "Valid and updated":
                case "Valid and not updated":
                    valid.add(providerBundle);
                    break;
                case "Not Audited":
                    notAudited.add(providerBundle);
                    break;
                case "Invalid and updated":
                    invalidAndUpdated.add(providerBundle);
                    break;
                case "Invalid and not updated":
                    invalidAndNotUpdated.add(providerBundle);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + auditVocStatus);
            }
        }
        for (String state : auditState){
            if (state.equals("Valid")){
                ret.addAll(valid);
            } else if (state.equals("Not Audited")){
                ret.addAll(notAudited);
            } else if (state.equals("Invalid and updated")){
                ret.addAll(invalidAndUpdated);
            } else if (state.equals("Invalid and not updated")) {
                ret.addAll(invalidAndNotUpdated);
            } else {
                throw new ValidationException(String.format("The audit state [%s] you have provided is wrong", state));
            }
        }
        return createCorrectQuantityFacets(ret, retPaging, ff.getQuantity(), ff.getFrom());
    }

    public Paging<ProviderBundle> createCorrectQuantityFacets(List<ProviderBundle> providerBundle, Paging<ProviderBundle> providerBundlePaging,
                                                              int quantity, int from){
        if (!providerBundle.isEmpty()) {
            List<ProviderBundle> retWithCorrectQuantity = new ArrayList<>();
            if (from == 0){
                if (quantity <= providerBundle.size()){
                    for (int i=from; i<=quantity-1; i++){
                        retWithCorrectQuantity.add(providerBundle.get(i));
                    }
                } else{
                    retWithCorrectQuantity.addAll(providerBundle);
                }
                providerBundlePaging.setTo(retWithCorrectQuantity.size());
            } else{
                boolean indexOutOfBound = false;
                if (quantity <= providerBundle.size()){
                    for (int i=from; i<quantity+from; i++){
                        try{
                            retWithCorrectQuantity.add(providerBundle.get(i));
                            if (quantity+from > providerBundle.size()){
                                providerBundlePaging.setTo(providerBundle.size());
                            } else{
                                providerBundlePaging.setTo(quantity+from);
                            }
                        } catch (IndexOutOfBoundsException e){
                            indexOutOfBound = true;
                            continue;
                        }
                    }
                    if (indexOutOfBound){
                        providerBundlePaging.setTo(providerBundle.size());
                    }
                } else{
                    retWithCorrectQuantity.addAll(providerBundle);
                    if (quantity+from > providerBundle.size()){
                        providerBundlePaging.setTo(providerBundle.size());
                    } else{
                        providerBundlePaging.setTo(quantity+from);
                    }
                }
            }
            providerBundlePaging.setFrom(from);
            providerBundlePaging.setResults(retWithCorrectQuantity);
            providerBundlePaging.setTotal(providerBundle.size());
        } else{
            providerBundlePaging.setResults(providerBundle);
            providerBundlePaging.setTotal(0);
            providerBundlePaging.setFrom(0);
            providerBundlePaging.setTo(0);
        }
        return providerBundlePaging;
    }

    // TODO: refactor / delete?...
    public List<Map<String, Object>> createQueryForProviderFilters (FacetFilter ff, String orderDirection, String orderField){
        String keyword = ff.getKeyword();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        String query; // TODO: Replace with StringBuilder
        if (ff.getFilter().entrySet().isEmpty()){
            query = "SELECT provider_id,catalogue_id FROM provider_view WHERE catalogue_id = '"+catalogueName+"'";
        } else{
            query = "SELECT provider_id,catalogue_id FROM provider_view WHERE";
        }

        boolean firstTime = true;
        for (Map.Entry<String, Object> entry : ff.getFilter().entrySet()) {
            in.addValue(entry.getKey(), entry.getValue());
            // suspended
            if (entry.getKey().equals("suspended")) {
                if (firstTime) {
                    query += String.format(" (suspended=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    query += String.format(" AND (suspended=%s)", entry.getValue().toString());
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR suspended='");
                }
            }
            // active
            if (entry.getKey().equals("active")) {
                if (firstTime) {
                    query += String.format(" (active=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    query += String.format(" AND (active=%s)", entry.getValue().toString());
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR active='");
                }
            }
            // published
            if (entry.getKey().equals("published")) {
                if (firstTime) {
                    query += String.format(" (published=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    query += String.format(" AND (published=%s)", entry.getValue().toString());
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR published='");
                }
            }
            // status
            if (entry.getKey().equals("status")) {
                if (firstTime) {
                    query += String.format(" (status=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    query += String.format(" AND (status=%s)", entry.getValue().toString());
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR status='");
                }
            }
            // templateStatus
            if (entry.getKey().equals("templateStatus")) {
                if (firstTime) {
                    query += String.format(" (templateStatus=%s)", entry.getValue().toString());
                    firstTime = false;
                } else {
                    query += String.format(" AND (templateStatus=%s)", entry.getValue().toString());
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR templateStatus='");
                }
            }
            // catalogue_id
            if (entry.getKey().equals("catalogue_id")) {
                if (firstTime) {
                    if (((LinkedHashSet) entry.getValue()).contains("all")){
                        query += String.format(" (catalogue_id LIKE '%%%%')");
                        firstTime = false;
                        continue;
                    } else{
                        query += String.format(" (catalogue_id=%s)", entry.getValue().toString());
                        firstTime = false;
                    }
                } else {
                    if (((LinkedHashSet) entry.getValue()).contains("all")){
                        query += String.format(" AND (catalogue_id LIKE '%%%%')");
                        continue;
                    } else{
                        query += String.format(" AND (catalogue_id=%s)", entry.getValue().toString());
                    }
                }
                if (query.contains(",")){
                    query = query.replaceAll(", ", "' OR catalogue_id='");
                }
            }
        }

        // keyword on search bar
        if (keyword != null && !keyword.equals("")){
            // replace apostrophes to avoid bad sql grammar
            if (keyword.contains("'")){
                keyword = keyword.replaceAll("'", "''");
            }
            query += String.format(" AND upper(CONCAT(%s))", columnsOfInterest) + " like '%" + String.format("%s", keyword.toUpperCase()) + "%'";
        }

        // order/orderField
        if (orderField != null && !orderField.equals("")){
            query += String.format(" ORDER BY %s", orderField);
        } else{
            query += " ORDER BY name";
        }
        if (orderDirection !=null && !orderDirection.equals("")){
            query += String.format(" %s", orderDirection);
        }

        query = query.replaceAll("\\[", "'").replaceAll("\\]","'");
        logger.debug(query);

        return namedParameterJdbcTemplate.queryForList(query, in);
    }

    public Resource getResource(String providerId, String catalogueId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("provider_id = \"%s\" AND catalogue_id = \"%s\"", providerId, catalogueId), resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    private ProviderBundle onboard(ProviderBundle provider, String catalogueId, Authentication auth) {
        // create LoggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(provider, auth);
        provider.setLoggingInfo(loggingInfoList);
        if (catalogueId == null || catalogueId.equals("") || catalogueId.equals(catalogueName)) {
            // set catalogueId = eosc
            provider.getProvider().setCatalogueId(catalogueName);
            provider.setActive(false);
            provider.setStatus(vocabularyService.get("pending provider").getId());
            provider.setTemplateStatus(vocabularyService.get("no template status").getId());
        } else {
            commonMethods.checkCatalogueIdConsistency(provider, catalogueId);
            provider.setActive(true);
            provider.setStatus(vocabularyService.get("approved provider").getId());
            provider.setTemplateStatus(vocabularyService.get("approved template").getId());
            loggingInfoList.add(commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey()));
        }

        // latestOnboardingInfo
        provider.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size()-1));

        return provider;
    }

    private void addApprovedProviderToHLEVocabulary(ProviderBundle providerBundle){
        Vocabulary newHostingLegalEntity = new Vocabulary();
        newHostingLegalEntity.setId("provider_hosting_legal_entity-"+providerBundle.getProvider().getId());
        newHostingLegalEntity.setName(providerBundle.getProvider().getName());
        newHostingLegalEntity.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
        newHostingLegalEntity.setExtras(new HashMap<String, String>() {{put("catalogueId", providerBundle.getProvider().getCatalogueId());}});
        logger.info(String.format("Creating a new Hosting Legal Entity Vocabulary with id: [%s] and name: [%s]",
                newHostingLegalEntity.getId(), newHostingLegalEntity.getName()));
        vocabularyService.add(newHostingLegalEntity, null);
    }

    private void checkAndAddProviderToHLEVocabulary(ProviderBundle providerBundle){
        List<Vocabulary> allHLE = vocabularyService.getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        List<String> allHLEIDs = new ArrayList<>();
        List<String> allHLENames = new ArrayList<>();
        for (Vocabulary voc : allHLE){
            allHLEIDs.add(voc.getId());
            allHLENames.add(voc.getName());
        }
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.getProvider().isLegalEntity()){
            if (!allHLEIDs.contains("provider_hosting_legal_entity-" + providerBundle.getProvider().getId()) &&
                    !allHLENames.contains(providerBundle.getProvider().getName())) {
                addApprovedProviderToHLEVocabulary(providerBundle);
            }
        }
    }

    public Paging<?> getRejectedResources(FacetFilter ff, String resourceType, Authentication auth){
        List<Bundle<?>> ret = new ArrayList<>();
        if (resourceType.equals("service")){
            Browsing<ServiceBundle> providerRejectedResources = getResourceBundles(ff, serviceBundleService, auth);
            ret.addAll(providerRejectedResources.getResults());
            return new Paging<>(providerRejectedResources.getTotal(), providerRejectedResources.getFrom(),
                    providerRejectedResources.getTo(), ret, providerRejectedResources.getFacets());
        } else if (resourceType.equals("training_resource")){
            Browsing<TrainingResourceBundle> providerRejectedResources = getResourceBundles(ff, trainingResourceService, auth);
            ret.addAll(providerRejectedResources.getResults());
            return new Paging<>(providerRejectedResources.getTotal(), providerRejectedResources.getFrom(),
                    providerRejectedResources.getTo(), ret, providerRejectedResources.getFacets());
        }
        return null;
    }

    private <T extends Bundle<?>, I extends ResourceCRUDService<T, Authentication>> Browsing<T> getResourceBundles(FacetFilter ff, I service, Authentication auth) {
        FacetFilter filter = new FacetFilter();
        filter.setFrom(ff.getFrom());
        filter.setQuantity(ff.getQuantity());
        filter.setKeyword(ff.getKeyword());
        filter.setFilter(ff.getFilter());
        filter.setOrderBy(ff.getOrderBy());
        // Get all Catalogue's Resources
        return service.getAll(filter, auth);
    }

    public ProviderBundle createPublicProvider(ProviderBundle providerBundle, Authentication auth){
        publicProviderManager.add(providerBundle, auth);
        return providerBundle;
    }

    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public ProviderBundle suspend(String providerId, String catalogueId, boolean suspend, Authentication auth) {
        ProviderBundle providerBundle = get(catalogueId, providerId, auth);
        commonMethods.suspensionValidation(providerBundle, catalogueId, providerId, suspend, auth);

        // Suspend Provider
        commonMethods.suspendResource(providerBundle, catalogueId, suspend, auth);
        super.update(providerBundle, auth);
        Objects.requireNonNull(cacheManager.getCache(CACHE_PROVIDERS)).clear();

        // Suspend Provider's resources
        List<ServiceBundle> services = serviceBundleService.getResourceBundles(catalogueId, providerId, auth).getResults();
        List<TrainingResourceBundle> trainingResources = trainingResourceService.getResourceBundles(catalogueId, providerId, auth).getResults();
        List<InteroperabilityRecordBundle> interoperabilityRecords = interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, providerId, auth).getResults();

        if (services != null && !services.isEmpty()) {
            for (ServiceBundle serviceBundle : services) {
                serviceBundleService.suspend(serviceBundle.getId(), catalogueId, suspend, auth);
            }
        }
        if (trainingResources != null && !trainingResources.isEmpty()) {
            for (TrainingResourceBundle trainingResourceBundle : trainingResources) {
                trainingResourceService.suspend(trainingResourceBundle.getId(), catalogueId, suspend, auth);
            }
        }
        if (interoperabilityRecords != null && !interoperabilityRecords.isEmpty()) {
            for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecords) {
                interoperabilityRecordService.suspend(interoperabilityRecordBundle.getId(), catalogueId, suspend, auth);
            }
        }

        return providerBundle;
    }

    public String determineHostingLegalEntity(String providerName) {
        List<Vocabulary> hostingLegalEntityList = vocabularyService.getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        for (Vocabulary hle : hostingLegalEntityList) {
            if (hle.getName().equalsIgnoreCase(providerName)) {
                return hle.getId();
            }
        }
        return null;
    }

    public List<MapValues> getAllResourcesUnderASpecificHLE(String hle, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("hosting_legal_entity", hle);
        ff.addFilter("published", false);
        List<MapValues> mapValuesList = new ArrayList<>();
        List<ProviderBundle> providers = getAll(ff, auth).getResults();
        List<ServiceBundle> services = new ArrayList<>();
        List<TrainingResourceBundle> trainingResources = new ArrayList<>();
        List<InteroperabilityRecordBundle> interoperabilityRecords = new ArrayList<>();
        createMapValuesForHLE(providers, "provider", mapValuesList);
        for (ProviderBundle providerBundle : providers) {
            services.addAll(serviceBundleService.getResourceBundles(providerBundle.getProvider().getCatalogueId(),
                    providerBundle.getId(), auth).getResults());
            trainingResources.addAll(trainingResourceService.getResourceBundles(providerBundle.getProvider().
                    getCatalogueId(), providerBundle.getId(), auth).getResults());
            interoperabilityRecords.addAll(interoperabilityRecordService.getResourceBundles(providerBundle.getProvider().
                    getCatalogueId(), providerBundle.getId(), auth).getResults());
        }
        createMapValuesForHLE(services, "service", mapValuesList);
        createMapValuesForHLE(trainingResources, "training_resource", mapValuesList);
        createMapValuesForHLE(interoperabilityRecords, "interoperability_record", mapValuesList);
        return mapValuesList;
    }

    private List<MapValues> createMapValuesForHLE(List<?> resources, String resourceType, List<MapValues> mapValuesList) {
        MapValues mapValues = new MapValues();
        mapValues.setKey(resourceType);
        List<eu.einfracentral.dto.Value> valueList = new ArrayList<>();
        for (Object obj : resources) {
            eu.einfracentral.dto.Value value = new eu.einfracentral.dto.Value();
            switch (resourceType) {
                case "provider":
                    ProviderBundle providerBundle = (ProviderBundle) obj;
                    value.setId(providerBundle.getId());
                    value.setName(providerBundle.getProvider().getName());
                    break;
                case "service":
                    ServiceBundle serviceBundle = (ServiceBundle) obj;
                    value.setId(serviceBundle.getId());
                    value.setName(serviceBundle.getService().getName());
                    break;
                case "training_resource":
                    TrainingResourceBundle trainingResourceBundle = (TrainingResourceBundle) obj;
                    value.setId(trainingResourceBundle.getId());
                    value.setName(trainingResourceBundle.getTrainingResource().getTitle());
                    break;
                case "interoperability_record":
                    InteroperabilityRecordBundle interoperabilityRecordBundle = (InteroperabilityRecordBundle) obj;
                    value.setId(interoperabilityRecordBundle.getId());
                    value.setName(interoperabilityRecordBundle.getInteroperabilityRecord().getTitle());
                    break;
                default:
                    break;
            }
            valueList.add(value);
        }
        mapValues.setValues(valueList);
        mapValuesList.add(mapValues);
        return mapValuesList;
    }
}
