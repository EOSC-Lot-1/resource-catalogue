package gr.uoa.di.madgik.resourcecatalogue.manager;

import static gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils.validateScientificDomains;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo.ActionType;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceRevision;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceRevisionBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceRevisionBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.HelpdeskService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.MigrationService;
import gr.uoa.di.madgik.resourcecatalogue.service.MonitoringService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.RegistrationMailService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.SynchronizerService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceRevisionService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.validators.FieldValidator;

@org.springframework.stereotype.Service
public class ResourceRevisionManager extends ResourceManager<ResourceRevisionBundle> implements ResourceRevisionService {

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
    private final MigrationService migrationService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericManager genericManager;
    @Autowired
    private FacetLabelService facetLabelService;
    @Autowired
    private FieldValidator fieldValidator;
    @Autowired
    private SearchService searchService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ResourceRevisionManager(ProviderService providerService,
                                   IdCreator idCreator, @Lazy SecurityService securityService,
                                   @Lazy RegistrationMailService registrationMailService,
                                   @Lazy VocabularyService vocabularyService,
                                   @Lazy HelpdeskService helpdeskService,
                                   @Lazy MonitoringService monitoringService,
                                   @Lazy ResourceInteroperabilityRecordService resourceInteroperabilityRecordService,
                                   CatalogueService catalogueService,
                                   ProviderResourcesCommonMethods commonMethods,
                                   GenericManager genericManager,
                                   @Lazy MigrationService migrationService) {
        super(ResourceRevisionBundle.class);
        this.providerService = providerService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.catalogueService = catalogueService;
        this.commonMethods = commonMethods;
        this.genericManager = genericManager;
        this.migrationService = migrationService;
    }

    @Override
    public String getResourceTypeName() {
        return "resource_revision";
    }
    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #resourceRevisionBundle.payload)")
    public ResourceRevisionBundle add(ResourceRevisionBundle resourceRevisionBundle, Authentication auth) {
        
        return null;
    }

    @Override
    public ResourceRevisionBundle update(ResourceRevisionBundle resourceRevisionBundle, String comment, Authentication auth) {

       

        return null;
    }
    @Override
    public ResourceRevisionBundle get(String id, String catalogueId) {
        Resource resource = getResource(id, catalogueId);
        if (resource == null) {
            throw new ResourceNotFoundException(String.format("Could not find ResourceRevision with id: %s and catalogueId: %s", id, catalogueId));
        }
        return deserialize(resource);
    }

	@Override
	public ResourceRevisionBundle verify(String id, String status, Boolean active, Authentication auth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceRevisionBundle publish(String id, Boolean active, Authentication auth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceRevisionBundle suspend(String id, boolean suspend, Authentication auth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceRevisionBundle audit(String id, String comment, ActionType actionType, Authentication auth) {
		// TODO Auto-generated method stub
		return null;
	}
}
