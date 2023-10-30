package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiers;
import eu.einfracentral.domain.InteroperabilityRecordBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.JmsService;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicInteroperabilityRecordManager")
public class PublicInteroperabilityRecordManager extends ResourceManager<InteroperabilityRecordBundle>
        implements ResourceCRUDService<InteroperabilityRecordBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicInteroperabilityRecordManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Autowired
    public PublicInteroperabilityRecordManager(JmsService jmsService, SecurityService securityService,
                                               ProviderResourcesCommonMethods commonMethods) {
        super(InteroperabilityRecordBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "interoperability_record";
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedUserException("Please log in.");
        }

        List<InteroperabilityRecordBundle> interoperabilityRecordBundleList = new ArrayList<>();
        Browsing<InteroperabilityRecordBundle> interoperabilityRecordBundleBrowsing = super.getAll(facetFilter, authentication);
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, interoperabilityRecordBundle.getId(),
                    interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()) && interoperabilityRecordBundle.getMetadata().isPublished()) {
                interoperabilityRecordBundleList.add(interoperabilityRecordBundle);
            }
        }
        return new Browsing<>(interoperabilityRecordBundleBrowsing.getTotal(), interoperabilityRecordBundleBrowsing.getFrom(),
                interoperabilityRecordBundleBrowsing.getTo(), interoperabilityRecordBundleList, interoperabilityRecordBundleBrowsing.getFacets());
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        String lowerLevelResourceId = interoperabilityRecordBundle.getId();
        Identifiers.createOriginalId(interoperabilityRecordBundle);
        interoperabilityRecordBundle.setId(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(interoperabilityRecordBundle.getId(), interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());

        // set providerId to Public
        interoperabilityRecordBundle.getInteroperabilityRecord().setProviderId(String.format("%s.%s",
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId()));

        interoperabilityRecordBundle.getMetadata().setPublished(true);
        // create PID and set it as Alternative Identifier
        commonMethods.determineResourceTypeAndCreateAlternativeIdentifierForPID(interoperabilityRecordBundle, "guidelines/");
        InteroperabilityRecordBundle ret;
        logger.info(String.format("Interoperability Record [%s] is being published with id [%s]", lowerLevelResourceId, interoperabilityRecordBundle.getId()));
        ret = super.add(interoperabilityRecordBundle, null);
        jmsService.convertAndSendTopic("interoperability_record.create", interoperabilityRecordBundle);
        return ret;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        InteroperabilityRecordBundle published = super.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
        InteroperabilityRecordBundle ret = super.get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, interoperabilityRecordBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // set providerId to Public
        ret.getInteroperabilityRecord().setProviderId(published.getInteroperabilityRecord().getProviderId());

        ret.getInteroperabilityRecord().setAlternativeIdentifiers(commonMethods.updateAlternativeIdentifiers(
                interoperabilityRecordBundle.getInteroperabilityRecord().getAlternativeIdentifiers(),
                published.getInteroperabilityRecord().getAlternativeIdentifiers()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Interoperability Record with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try{
            InteroperabilityRecordBundle publicInteroperabilityRecordBundle = get(String.format("%s.%s", interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId(), interoperabilityRecordBundle.getId()));
            logger.info(String.format("Deleting public Interoperability Record with id [%s]", publicInteroperabilityRecordBundle.getId()));
            super.delete(publicInteroperabilityRecordBundle);
            jmsService.convertAndSendTopic("interoperability_record.delete", publicInteroperabilityRecordBundle);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }

}
