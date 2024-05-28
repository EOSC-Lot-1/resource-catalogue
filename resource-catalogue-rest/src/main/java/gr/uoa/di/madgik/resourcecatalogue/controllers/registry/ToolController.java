package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.ToolService;
import gr.uoa.di.madgik.resourcecatalogue.service.ToolService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"tool"})
@Tag(name = "tool")
public class ToolController {

    private static final Logger logger = LogManager.getLogger(ToolController.class.getName());
    private final ToolService<ToolBundle> toolService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;
    private final GenericResourceService genericResourceService;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Value("${project.name:Resource Catalogue}")
    private String projectName;

    @Autowired
    ToolController(ToolService<ToolBundle> toolService,
                               ProviderService<ProviderBundle, Authentication> providerService,
                               DataSource commonDataSource, GenericResourceService genericResourceService) {
        this.toolService = toolService;
        this.providerService = providerService;
        this.commonDataSource = commonDataSource;
        this.genericResourceService = genericResourceService;
    }

    @DeleteMapping(path = {"{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ToolBundle> delete(@PathVariable("id") String id,
                                                         @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                         @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ToolBundle toolBundle;
        toolBundle = toolService.get(id, catalogueId);

        // Block users of deleting Services of another Catalogue
        //if (!toolBundle.getTool().getCatalogueId().equals(catalogueName)) {
        //    throw new ValidationException(String.format("You cannot delete a Tool of a non [%s] Catalogue.", projectName));
        //}
        //TODO: Maybe return Provider's template status to 'no template status' if this was its only TR
        toolService.delete(toolBundle);
        logger.info("User '{}' deleted Tool'{}' with id: '{}'", auth.getName(), toolBundle.getTool().getName(),
                toolBundle.getTool().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get the most current version of a specific Tool, providing the Resource id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("@securityService.toolIsActive(#id, #catalogueId) or hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<Tool> getTool(@PathVariable("id") String id, @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(toolService.get(id, catalogueId).getTool(), HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #id)")
    public ResponseEntity<ToolBundle> getToolBundle(@PathVariable("id") String id, @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(toolService.get(id, catalogueId), HttpStatus.OK);
    }

    @Operation(summary = "Creates a new Tool.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerCanAddResources(#auth, #tool)")
    public ResponseEntity<Tool> addTool(@RequestBody Tool tool, @Parameter(hidden = true) Authentication auth) {
        ToolBundle ret = this.toolService.addResource(new ToolBundle(tool), auth);
        logger.info("User '{}' created a new Tool with title '{}' and id '{}'", auth.getName(), tool.getName(), tool.getId());
        return new ResponseEntity<>(ret.getTool(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Tool assigned the given id with the given Tool, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth,#tool)")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Tool> updateTool(@RequestBody Tool tool, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ToolBundle ret = this.toolService.updateResource(new ToolBundle(tool), comment, auth);
        logger.info("User '{}' updated Tool with title '{}' and id '{}'", auth.getName(), tool.getName(), tool.getId());
        return new ResponseEntity<>(ret.getTool(), HttpStatus.OK);
    }

    // Accept/Reject a Resource.
    @PatchMapping(path = "verifyTool/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ToolBundle> verifyTool(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                                         @RequestParam(required = false) String status, @Parameter(hidden = true) Authentication auth) {
        ToolBundle toolBundle = toolService.verifyResource(id, status, active, auth);
        logger.info("User '{}' updated Tool with title '{}' [status: {}] [active: {}]", auth, toolBundle.getTool().getName(), status, active);
        return new ResponseEntity<>(toolBundle, HttpStatus.OK);
    }

    @Operation(summary = "Validates the Tool without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Tool tool) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(toolService.validateTool(new ToolBundle(tool)));
        logger.info("Validated Tool with title '{}' and id '{}'", tool.getName(), tool.getId());
        return ret;
    }

    @Operation(summary = "Filter a list of Tools based on a set of filters or get a list of all Tools in the Catalogue.")
    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Tool>> getAllTools(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("tool");
        ff.addFilter("published", false);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved resource");
        Paging<Tool> paging = genericResourceService.getResults(ff).map(r -> ((ToolBundle) r).getPayload());
        return ResponseEntity.ok(paging);
    }

    @GetMapping(path = "/childrenFromParent", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getChildrenFromParent(@RequestParam String type, @RequestParam String parent, @Parameter(hidden = true) Authentication auth) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(commonDataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();
        String query = "";
        if ("SCIENTIFIC_DOMAIN".equals(type)) {
            query = "SELECT scientific_subdomains FROM service_view";
        }
        List<Map<String, Object>> rec = namedParameterJdbcTemplate.queryForList(query, in);
        return toolService.getChildrenFromParent(type, parent, rec);
    }

    //    @Operation(summary = "Get a list of Tool based on a set of ids.")
    @Parameters({
            @Parameter(name = "ids", description = "Comma-separated list of Tool ids", content = @Content(schema = @Schema(type = "boolean", defaultValue = "")))
    })
    @GetMapping(path = "byID/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Tool>> getSomeTools(@PathVariable("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(toolService.getByIds(auth, ids));
    }

    @Operation(summary = "Get all Tools in the catalogue organized by an attribute, e.g. get Tools organized in categories.")
    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Tool>>> getToolsBy(@PathVariable(value = "field") Service.Field field, @Parameter(hidden = true) Authentication auth) throws NoSuchFieldException {
        Map<String, List<ToolBundle>> results;
        try {
            results = toolService.getBy(field.getKey(), auth);
        } catch (NoSuchFieldException e) {
            logger.error(e);
            throw e;
        }
        Map<String, List<Tool>> toolResults = new TreeMap<>();
        for (Map.Entry<String, List<ToolBundle>> toolBundles : results.entrySet()) {
            List<Tool> items = toolBundles.getValue()
                    .stream()
                    .map(ToolBundle::getTool).collect(Collectors.toList());
            if (!items.isEmpty()) {
                toolResults.put(toolBundles.getKey(), items);
            }
        }
        return ResponseEntity.ok(toolResults);
    }

    @Browse
    @GetMapping(path = "byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#id,#catalogueId)")
    public ResponseEntity<Paging<ToolBundle>> getToolsByProvider(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                         @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                                                         @PathVariable String id,
                                                                                         @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("tool");
        ff.addFilter("published", false);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("active", true);
        Paging<ToolBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @Browse
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<ToolBundle>> getToolsByCatalogue(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                                          @PathVariable String id,
                                                                                          @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("catalogue_id", id);
        ff.addFilter("published", false);
        return ResponseEntity.ok(toolService.getAll(ff, auth));
    }

    // Filter a list of inactive Tools based on a set of filters or get a list of all inactive Tools in the Catalogue.
    @Browse
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Tool>> getInactiveTools(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("active", false);
        Paging<ToolBundle> toolBundles = toolService.getAll(ff, auth);
        List<Tool> tools = toolBundles.getResults().stream().map(ToolBundle::getTool).collect(Collectors.toList());
        if (tools.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return ResponseEntity.ok(new Paging<>(toolBundles.getTotal(), toolBundles.getFrom(), toolBundles.getTo(), tools, toolBundles.getFacets()));
    }

    // Providing the Tool id, set the Tool to active or inactive.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<ToolBundle> setActive(@PathVariable String id, @RequestParam Boolean active, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to save Tool with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return ResponseEntity.ok(toolService.publish(id, active, auth));
    }

    // Get all pending Service Templates.
    @GetMapping(path = "pending/all", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Browsing<Tool>> pendingTemplates(@Parameter(hidden = true) Authentication auth) {
        List<ProviderBundle> pendingProviders = providerService.getInactive();
        List<Tool> serviceTemplates = new ArrayList<>();
        for (ProviderBundle provider : pendingProviders) {
            if (provider.getTemplateStatus().equals("pending template")) {
                serviceTemplates.addAll(toolService.getInactiveResources(provider.getId()).stream().map(ToolBundle::getTool).collect(Collectors.toList()));
            }
        }
        Browsing<Tool> tools = new Browsing<>(serviceTemplates.size(), 0, serviceTemplates.size(), serviceTemplates, null);
        return ResponseEntity.ok(tools);
    }

    @Browse
    @BrowseCatalogue
    @Parameter(name = "suspended", description = "Suspended", content = @Content(schema = @Schema(type = "boolean", defaultValue = "false")))
    @GetMapping(path = "adminPage/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ToolBundle>> getAllToolsForAdminPage(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.setResourceType("tool");
        ff.addFilter("published", false);
        Paging<ToolBundle> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PatchMapping(path = "auditResource/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ToolBundle> auditResource(@PathVariable("id") String id,
                                                                @RequestParam("catalogueId") String catalogueId,
                                                                @RequestParam(required = false) String comment,
                                                                @RequestParam LoggingInfo.ActionType actionType,
                                                                @Parameter(hidden = true) Authentication auth) {
        ToolBundle tool = toolService.auditResource(id, catalogueId, comment, actionType, auth);
        return new ResponseEntity<>(tool, HttpStatus.OK);
    }


    @Parameters({
            @Parameter(name = "quantity", description = "Quantity to be fetched", content = @Content(schema = @Schema(type = "string", defaultValue = "10")))
    })
    @GetMapping(path = "randomResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ToolBundle>> getRandomResources(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("status", "approved resource");
        ff.addFilter("published", false);
        Paging<ToolBundle> toolBundlePaging = toolService.getRandomResources(ff, auditingInterval, auth);
        return new ResponseEntity<>(toolBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Resource based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,
                                                                  @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        Paging<LoggingInfo> loggingInfoHistory = this.toolService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    // Send emails to Providers whose Resources are outdated
    @GetMapping(path = {"sendEmailForOutdatedResource/{resourceId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void sendEmailNotificationsToProvidersWithOutdatedResources(@PathVariable String resourceId, @Parameter(hidden = true) Authentication authentication) {
        toolService.sendEmailNotificationsToProvidersWithOutdatedResources(resourceId, authentication);
    }

    // Move a Tool to another Provider
    @PostMapping(path = {"changeProvider"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void changeProvider(@RequestParam String resourceId, @RequestParam String newProvider, @RequestParam(required = false) String comment, @Parameter(hidden = true) Authentication authentication) {
        toolService.changeProvider(resourceId, newProvider, comment, authentication);
    }

    @Browse
    @GetMapping(path = "getSharedResources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isProviderAdmin(#auth,#id)")
    public ResponseEntity<Paging<ToolBundle>> getSharedResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams, @PathVariable String id, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("resource_providers", id);
        return ResponseEntity.ok(toolService.getAll(ff, null));
    }

    // Create a Public Tool if something went bad during its creation
    @Hidden
    @PostMapping(path = "createPublicTool", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ToolBundle> createPublicTool(@RequestBody ToolBundle toolBundle, @Parameter(hidden = true) Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Tool from Tool '{}'-'{}'", User.of(auth).getFullName(),
                User.of(auth).getEmail(), toolBundle.getId(), toolBundle.getTool().getName());
        return ResponseEntity.ok(toolService.createPublicResource(toolBundle, auth));
    }

    @PostMapping(path = "addToolBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ToolBundle> add(@RequestBody ToolBundle toolBundle, Authentication authentication) {
        ResponseEntity<ToolBundle> ret = new ResponseEntity<>(toolService.add(toolBundle, authentication), HttpStatus.OK);
        logger.info("User '{}' added ToolBundle '{}' with id: {}", authentication, toolBundle.getTool().getName(), toolBundle.getTool().getId());
        return ret;
    }

    @PutMapping(path = "updateToolBundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ToolBundle> update(@RequestBody ToolBundle toolBundle, @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        ResponseEntity<ToolBundle> ret = new ResponseEntity<>(toolService.update(toolBundle, authentication), HttpStatus.OK);
        logger.info("User '{}' updated ToolBundle '{}' with id: {}", authentication, toolBundle.getTool().getName(), toolBundle.getTool().getId());
        return ret;
    }

    @Operation(summary = "Suspends a specific Tool .")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ToolBundle suspendTool(@RequestParam String toolId, @RequestParam String catalogueId, @RequestParam boolean suspend, @Parameter(hidden = true) Authentication auth) {
        return toolService.suspend(toolId, catalogueId, suspend, auth);
    }

}
