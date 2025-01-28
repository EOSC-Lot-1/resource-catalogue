package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Tool;
import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ToolService extends ResourceService<ToolBundle>, BundleOperations<ToolBundle> {

    /**
     * Add a new Tool
     *
     * @param resource    Tool
     * @param auth        Authentication
     * @return {@link   ToolBundle}
     */
    ToolBundle add(ToolBundle resource, Authentication auth);

    /**
     * Update a Tool of the EOSC Catalogue.
     *
     * @param resource Tool
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link   ToolBundle}
     * @throws ResourceNotFoundException The Resource was not found
     */
    ToolBundle update(ToolBundle resource, String comment, Authentication auth) throws ResourceNotFoundException;


    /**
     * Get a Tool of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param resourceId  Tool ID
     * @param auth        Authentication
     * @return {@link   ToolBundle}
     */
    ToolBundle getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Tool with the specified ID
     *
     * @param id          Tool ID
     * @param catalogueId Catalogue ID
     * @return {@link   ToolBundle}
     */
    ToolBundle get(String id, String catalogueId);

    /**
     * Get Tool Bundles by a specific field.
     *
     * @param field Field of Tool
     * @param auth  Authentication
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link   ToolBundle}&gt;&gt;
     * @throws NoSuchFieldException The field does not exist
     */
    Map<String, List<ToolBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get Tools with the specified ids.
     *
     * @param authentication Authentication
     * @param ids            Tool IDs
     * @return {@link List}&lt;{@link Tool}&gt;
     */
    List<Tool> getByIds(Authentication authentication, String... ids);

    /**
     * Validates the given Tool Bundle.
     *
     * @param trainingResourceBundle Tool Bundle
     * @return True/False
     */
    boolean validateTool(ToolBundle trainingResourceBundle);

    /**
     * Return children vocabularies from parent vocabularies
     *
     * @param type   Vocabulary's type
     * @param parent Vocabulary's parent
     * @param rec
     * @return {@link List}&lt;{@link String}&gt;
     */
    List<String> getChildrenFromParent(String type, String parent, List<Map<String, Object>> rec);

    /**
     * Gets a Browsing of all Tools for admins
     *
     * @param filter FacetFilter
     * @param auth   Authentication
     * @return {@link Browsing}&lt;{@link   ToolBundle}&gt;
     */
    Browsing<ToolBundle> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * Get a paging of random Tools
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link   ToolBundle}&gt;
     */
    Paging<ToolBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Tool Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link   ToolBundle}&gt;
     */
    List<ToolBundle> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Tool Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link   ToolBundle}&gt;
     */
    Paging<ToolBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth);

    /**
     * Get a list of Tools of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link Tool}&gt;
     */
    List<? extends Tool> getResources(String providerId, Authentication auth);

    /**
     * Get an EOSC Provider's Tool Template, if exists, else return null
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link ToolBundle}
     */
    ToolBundle getResourceTemplate(String providerId, Authentication auth);

    /**
     * Get all inactive Tools of a specific Provider, providing its ID
     *
     * @param providerId Provider ID
     * @return {@link List}&lt;{@link   ToolBundle}&gt;
     */
    List<ToolBundle> getInactiveResources(String providerId);

    /**
     * Send email notifications to all Providers with outdated Tools
     *
     * @param resourceId Tool ID
     * @param auth       Authentication
     */
    void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth);

    /**
     * Get the history of the specific Tool of the specific Catalogue ID
     *
     * @param id          Tool ID
     * @param catalogueId Catalogue ID
     * @return {@link Paging}&lt;{@link LoggingInfo}&gt;
     */
    Paging<LoggingInfo> getLoggingInfoHistory(String id, String catalogueId);

    /**
     * Change the Provider of the specific Tool
     *
     * @param resourceId  Tool ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link   ToolBundle}
     */
    ToolBundle changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

    /**
     * Get a specific Tool of the EOSC Catalogue, given its ID, or return null
     *
     * @param id Tool ID
     * @return {@link ToolBundle}
     */
    ToolBundle getOrElseReturnNull(String id);

    /**
     * Get a specific Tool of an external Catalogue, given its ID, or return null
     *
     * @param id          Tool ID
     * @param catalogueId Catalogue ID
     * @return {@link ToolBundle}
     */
    ToolBundle getOrElseReturnNull(String id, String catalogueId);
    
    /**
     * Get a list of Tools by date-status
     *
     * @param id Tool ID
     * @return {@link ToolBundle}
     */
    List <ToolBundle> getToolsByDateStatus(String date, String status, List <ToolBundle> tools);

    /**
     * Create a Public Tool
     *
     * @param resource Tool
     * @param auth     Authentication
     * @return {@link   ToolBundle}
     */
    ToolBundle createPublicResource(ToolBundle resource, Authentication auth);

    /**
     * Publish Tool's related resources
     *
     * @param id          Tool ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishToolRelatedResources(String id, Boolean active,
                                                 Authentication auth);
}
