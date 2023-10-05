package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiers;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.einfracentral.utils.JmsService;
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

@Service("publicProviderManager")
public class PublicProviderManager extends ResourceManager<ProviderBundle> implements ResourceCRUDService<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicProviderManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Autowired
    public PublicProviderManager(JmsService jmsService, SecurityService securityService,
                                 ProviderResourcesCommonMethods commonMethods) {
        super(ProviderBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public Browsing<ProviderBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<ProviderBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedUserException("Please log in.");
        }

        List<ProviderBundle> providerList = new ArrayList<>();
        Browsing<ProviderBundle> providerBundleBrowsing = super.getAll(facetFilter, authentication);
        for (ProviderBundle providerBundle : providerBundleBrowsing.getResults()) {
            if (securityService.isProviderAdmin(authentication, providerBundle.getId(), providerBundle.getProvider().getCatalogueId()) && providerBundle.getMetadata().isPublished()) {
                providerList.add(providerBundle);
            }
        }
        return new Browsing<>(providerBundleBrowsing.getTotal(), providerBundleBrowsing.getFrom(),
                providerBundleBrowsing.getTo(), providerList, providerBundleBrowsing.getFacets());
    }

    @Override
    public ProviderBundle add(ProviderBundle providerBundle, Authentication authentication) {
        String lowerLevelProviderId = providerBundle.getId();
        Identifiers.createOriginalId(providerBundle);
        providerBundle.setId(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(providerBundle.getId(), providerBundle.getProvider().getCatalogueId());
        providerBundle.getMetadata().setPublished(true);
        // create PID and set it as Alternative Identifier
        providerBundle.getIdentifiers().setAlternativeIdentifiers(commonMethods.createAlternativeIdentifierForPID(providerBundle, "providers/"));
        ProviderBundle ret;
        logger.info(String.format("Provider [%s] is being published with id [%s]", lowerLevelProviderId, providerBundle.getId()));
        ret = super.add(providerBundle, null);
        jmsService.convertAndSendTopic("provider.create", providerBundle);
        return ret;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication authentication) {
        ProviderBundle published = super.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        ProviderBundle ret = super.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, providerBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        ret.setIdentifiers(commonMethods.updateAlternativeIdentifiers(providerBundle, published));
        ret.setId(published.getId());
        ret.setMetadata(published.getMetadata());
        logger.info(String.format("Updating public Provider with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("provider.update", ret);
        return ret;
    }

    @Override
    public void delete(ProviderBundle providerBundle) {
        try{
            ProviderBundle publicProviderBundle = get(String.format("%s.%s",providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            logger.info(String.format("Deleting public Provider with id [%s]", publicProviderBundle.getId()));
            super.delete(publicProviderBundle);
            jmsService.convertAndSendTopic("provider.delete", publicProviderBundle);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }
}