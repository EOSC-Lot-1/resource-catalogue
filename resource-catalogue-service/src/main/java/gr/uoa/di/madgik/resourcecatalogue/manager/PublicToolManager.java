package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicToolManager")
public class PublicToolManager extends AbstractPublicResourceManager<ToolBundle> implements ResourceCRUDService<ToolBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicToolManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;

    @Autowired
    public PublicToolManager(JmsService jmsService, SecurityService securityService,
                                         ProviderResourcesCommonMethods commonMethods,
                                         FacetLabelService facetLabelService) {
        super(ToolBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceType() {
        return "tool";
    }

    @Override
    public Browsing<ToolBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<ToolBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public Browsing<ToolBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<ToolBundle> toolBundleList = new ArrayList<>();
        Browsing<ToolBundle> toolBundleBrowsing = super.getAll(facetFilter, authentication);
        for (ToolBundle toolBundle : toolBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, toolBundle.getId()) && toolBundle.getMetadata().isPublished()) {
                toolBundleList.add(toolBundle);
            }
        }
        return new Browsing<>(toolBundleBrowsing.getTotal(), toolBundleBrowsing.getFrom(),
                toolBundleBrowsing.getTo(), toolBundleList, toolBundleBrowsing.getFacets());
    }

    @Override
    public ToolBundle add(ToolBundle toolBundle, Authentication authentication) {
        String lowerLevelResourceId = toolBundle.getId();
        Identifiers.createOriginalId(toolBundle);
        toolBundle.setId(String.format("%s", toolBundle.getId()));
        //commonMethods.restrictPrefixRepetitionOnPublicResources(toolBundle.getId(), toolBundle.getTool().getCatalogueId());

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateToolIdsToPublic(toolBundle);

        toolBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
//        for (AlternativeIdentifier alternativeIdentifier : toolBundle.getTool().getAlternativeIdentifiers()) {
//            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
//                pid = alternativeIdentifier.getValue();
//                break;
//            }
//        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Tool with id {} does not have a PID registered under its AlternativeIdentifiers.",
                    toolBundle.getId());
        } else {
            commonMethods.postPID(pid);
        }
        ToolBundle ret;
        logger.info(String.format("Tool [%s] is being published with id [%s]", lowerLevelResourceId, toolBundle.getId()));
        ret = super.add(toolBundle, null);
        jmsService.convertAndSendTopic("tool.create", toolBundle);
        return ret;
    }

    @Override
    public ToolBundle update(ToolBundle toolBundle, Authentication authentication) {
        ToolBundle published = super.get(String.format("%s", toolBundle.getId()));
        ToolBundle ret = super.get(String.format("%s",  toolBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, toolBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateToolIdsToPublic(ret);

        //ret.getTool().setAlternativeIdentifiers(commonMethods.updateAlternativeIdentifiers(
        //       toolBundle.getTool().getAlternativeIdentifiers(),
        //        published.getTool().getAlternativeIdentifiers()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Tool with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("tool.update", ret);
        return ret;
    }

    @Override
    public void delete(ToolBundle toolBundle) {
        try {
            ToolBundle publicToolBundle = get(String.format("%s", toolBundle.getId()));
            logger.info(String.format("Deleting public Tool with id [%s]", publicToolBundle.getId()));
            super.delete(publicToolBundle);
            jmsService.convertAndSendTopic("tool.delete", publicToolBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
