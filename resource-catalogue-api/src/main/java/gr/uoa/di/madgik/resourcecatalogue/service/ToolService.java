package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Tool;
import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface ToolService<T> extends ResourceService<T, Authentication> {

    /**
     * Add a new Tool on the EOSC Catalogue
     *
     * @param resource Tool
     * @param auth     Authentication
     * @return {@link T}
     */
    T addResource(T resource, Authentication auth);

    /**
     * Add a new Tool on an external Catalogue, providing the Catalogue's ID
     *
     * @param resource    Tool
     * @param catalogueId Catalogue ID
     * @param auth        Authentication
     * @return {@link T}
     */
    T addResource(T resource, String catalogueId, Authentication auth);

    /**
     * Update a Tool of the EOSC Catalogue.
     *
     * @param resource Tool
     * @param comment  Comment
     * @param auth     Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException The Resource was not found
     */
    T updateResource(T resource, String comment, Authentication auth) throws ResourceNotFoundException;

    /**
     * Update a Tool of an external Catalogue, providing its Catalogue ID
     *
     * @param resource    Tool
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link T}
     * @throws ResourceNotFoundException The Resource was not found
     */
    T updateResource(T resource, String catalogueId, String comment, Authentication auth)
            throws ResourceNotFoundException;

    /**
     * Get a Tool of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param resourceId  ToolID
     * @param auth        Authentication
     * @return {@link T}
     */
    T getCatalogueResource(String catalogueId, String resourceId, Authentication auth);

    /**
     * Returns the Tool with the specified ID
     *
     * @param id          Tool ID
     * @param catalogueId Catalogue ID
     * @return {@link T}
     */
    T get(String id, String catalogueId);

    /**
     * Get Tool Bundles by a specific field.
     *
     * @param field Field of Tool
     * @param auth  Authentication
     * @return {@link Map}&lt;{@link String},{@link List}&lt;{@link T}&gt;&gt;
     * @throws NoSuchFieldException The field does not exist
     */
    Map<String, List<T>> getBy(String field, Authentication auth) throws NoSuchFieldException;

    /**
     * Get Tools with the specified ids.
     *
     * @param authentication Authentication
     * @param ids            Tool IDs
     * @return {@link List}&lt;{@link Tool}&gt;
     */
    List<Tool> getByIds(Authentication authentication, String... ids);


    /**
     * Check if the Tool exists.
     *
     * @param ids Tool IDs
     * @return True/False
     */
    boolean exists(SearchService.KeyValue... ids);

    /**
     * Validates the given Tool Bundle.
     *
     * @param ToolBundle Tool Bundle
     * @return True/False
     */
    boolean validateTool(ToolBundle ToolBundle);

    /**
     * Sets a Tool as active/inactive.
     *
     * @param resourceId Tool ID
     * @param active     True/False
     * @param auth       Authentication
     * @return {@link T}
     */
    T publish(String resourceId, Boolean active, Authentication auth);

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
     * @return {@link Browsing}&lt;{@link T}&gt;
     */
    Browsing<T> getAllForAdmin(FacetFilter filter, Authentication auth);

    /**
     * Audit a Tool
     *
     * @param resourceId  Tool ID
     * @param catalogueId Catalogue ID
     * @param comment     Comment
     * @param actionType  Audit's action type
     * @param auth        Authentication
     * @return {@link T}
     */
    T auditResource(String resourceId, String catalogueId, String comment, LoggingInfo.ActionType actionType,
                    Authentication auth);

    /**
     * Get a paging of random Tools
     *
     * @param ff               FacetFilter
     * @param auditingInterval Auditing Interval (in months)
     * @param auth             Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth);

    /**
     * Get a list of Tool Bundles of a specific Provider of the EOSC Catalogue
     *
     * @param providerId Provider ID
     * @param auth       Authentication
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getResourceBundles(String providerId, Authentication auth);

    /**
     * Get a paging of Tool Bundles of a specific Provider of an external Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param providerId  Provider ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link T}&gt;
     */
    Paging<T> getResourceBundles(String catalogueId, String providerId, Authentication auth);

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
     * @return {@link List}&lt;{@link T}&gt;
     */
    List<T> getInactiveResources(String providerId);

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
     * Verify the Tool providing its ID
     *
     * @param id     Tool ID
     * @param status Tool's status (approved/rejected)
     * @param active True/False
     * @param auth   Authentication
     * @return {@link T}
     */
    T verifyResource(String id, String status, Boolean active, Authentication auth);

    /**
     * Change the Provider of the specific Tool
     *
     * @param resourceId  Tool ID
     * @param newProvider New Provider ID
     * @param comment     Comment
     * @param auth        Authentication
     * @return {@link T}
     */
    T changeProvider(String resourceId, String newProvider, String comment, Authentication auth);

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
     * Create a Public Tool
     *
     * @param resource Tool
     * @param auth     Authentication
     * @return {@link T}
     */
    T createPublicResource(T resource, Authentication auth);

    /**
     * Suspend the Tool given its ID
     *
     * @param ToolId Tool ID
     * @param catalogueId        Catalogue ID
     * @param suspend            True/False
     * @param auth               Authentication
     * @return {@link ToolBundle}
     */
    ToolBundle suspend(String ToolId, String catalogueId, boolean suspend, Authentication auth);

    /**
     * Publish Tool's related resources
     *
     * @param id          Tool ID
     * @param catalogueId Catalogue ID
     * @param active      True/False
     * @param auth        Authentication
     */
    void publishToolRelatedResources(String id, Boolean active, Authentication auth);
}
