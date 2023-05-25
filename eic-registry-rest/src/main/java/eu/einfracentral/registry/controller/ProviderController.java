package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.MigrationService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("provider")
@Api(value = "Get information about a Provider")
public class ProviderController {

    private static final Logger logger = LogManager.getLogger(ProviderController.class);
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final SecurityService securityService;
    private final MigrationService migrationService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Value("${auditing.interval:6}")
    private String auditingInterval;

    @Autowired
    ProviderController(ProviderService<ProviderBundle, Authentication> service,
                       ResourceBundleService<ServiceBundle> resourceBundleService,
                       ResourceBundleService<DatasourceBundle> datasourceBundleService,
                       TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                       SecurityService securityService, MigrationService migrationService) {
        this.providerService = service;
        this.resourceBundleService = resourceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.securityService = securityService;
        this.migrationService = migrationService;
    }

    // Deletes the Provider with the given id.
    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Provider> delete(@PathVariable("id") String id,
                                           @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                           @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerService.get(catalogueId, id, auth);
        if (provider == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        // Block users of deleting Providers of another Catalogue
        if (!provider.getProvider().getCatalogueId().equals(catalogueName)){
            throw new ValidationException("You cannot delete a Provider of a non EOSC Catalogue.");
        }
        logger.info("Deleting provider: {} of the catalogue: {}", provider.getProvider().getName(), provider.getProvider().getCatalogueId());

        // delete Provider
        providerService.delete(provider);
        logger.info("User '{}' deleted the Provider with name '{}' and id '{}'", auth.getName(), provider.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(provider.getProvider(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Provider with the given id.")
    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id,
                                        @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                        @ApiIgnore Authentication auth) {
        Provider provider = providerService.get(catalogueId, id, auth).getProvider();
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Creates a new Provider.
//    @Override
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> add(@RequestBody Provider provider, @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerService.add(new ProviderBundle(provider), auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> addBundle(@RequestBody ProviderBundle provider, @ApiIgnore Authentication auth) {
        ProviderBundle providerBundle = providerService.add(provider, auth);
        logger.info("User '{}' added the Provider with name '{}' and id '{}'", auth.getName(), providerBundle.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.CREATED);
    }

    //    @Override
    @ApiOperation(value = "Updates the Provider assigned the given id with the given Provider, keeping a version of revisions.")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#provider.id,#provider.catalogueId)")
    public ResponseEntity<Provider> update(@RequestBody Provider provider,
                                           @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                           @RequestParam(required = false) String comment,
                                           @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerService.get(catalogueId, provider.getId(), auth);
        providerBundle.setProvider(provider);
        if (comment == null || comment.equals("")) {
            comment = "no comment";
        }
        providerBundle = providerService.update(providerBundle, comment, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{}'", auth.getName(), provider.getName(), provider.getId());
        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/bundle", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> updateBundle(@RequestBody ProviderBundle provider, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = providerService.update(provider, auth);
        logger.info("User '{}' updated the Provider with name '{}' and id '{}'", auth.getName(), providerBundle.getProvider().getName(), provider.getId());
        return new ResponseEntity<>(providerBundle, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Provider>> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                   @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueIds,
                                                   @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        ff.addFilter("published", false);
        List<Provider> providerList = new LinkedList<>();
        Paging<ProviderBundle> providerBundlePaging = providerService.getAll(ff, auth);
        for (ProviderBundle providerBundle : providerBundlePaging.getResults()) {
            providerList.add(providerBundle.getProvider());
        }
        Paging<Provider> providerPaging = new Paging<>(providerBundlePaging.getTotal(), providerBundlePaging.getFrom(),
                providerBundlePaging.getTo(), providerList, providerBundlePaging.getFacets());
        return new ResponseEntity<>(providerPaging, HttpStatus.OK);
    }

    @GetMapping(path = "bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #id, #catalogueId)")
    public ResponseEntity<ProviderBundle> getProviderBundle(@PathVariable("id") String id,
                                                            @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                            @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(providerService.get(catalogueId, id, auth), HttpStatus.OK);
    }

    // Filter a list of Providers based on a set of filters or get a list of all Providers in the Catalogue.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "bundle/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getAllProviderBundles(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth,
                                                                        @RequestParam(required = false) Set<String> status, @RequestParam(required = false) Set<String> templateStatus,
                                                                        @RequestParam(required = false) Set<String> auditState, @RequestParam(required = false) Set<String> catalogue_id) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(allRequestParams.get("query") != null ? (String) allRequestParams.remove("query") : "");
        ff.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String) allRequestParams.remove("from")) : 0);
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = allRequestParams.get("order") != null ? (String) allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String) allRequestParams.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        if (status != null) {
            ff.addFilter("status", status);
        }
        if (templateStatus != null) {
            ff.addFilter("templateStatus", templateStatus);
        }
        Set<String> catalogueNameToSet = new LinkedHashSet<>();
        if (catalogue_id != null) {
            if (catalogue_id.contains("all")) {
                catalogueNameToSet.add("all");
                ff.addFilter("catalogue_id", catalogueNameToSet);
            } else{
                ff.addFilter("catalogue_id", catalogue_id);
            }
        } else{
            catalogueNameToSet.add(catalogueName);
            ff.addFilter("catalogue_id", catalogueNameToSet);
        }
        ff.addFilter("published", false);

        List<Map<String, Object>> records = providerService.createQueryForProviderFilters(ff, orderDirection, orderField);
        List<ProviderBundle> ret = new ArrayList<>();
        Paging<ProviderBundle> retPaging = providerService.getAll(ff, auth);
        if (records != null && !records.isEmpty()){
            for (Map<String, Object> record : records){
                ret.add(providerService.get((String) record.get("catalogue_id"), (String) record.get("provider_id"), auth));
            }
        }
        if (auditState == null){
            return ResponseEntity.ok(providerService.createCorrectQuantityFacets(ret, retPaging, ff.getQuantity(), ff.getFrom()));
        } else{
            Paging<ProviderBundle> retWithAuditState = providerService.determineAuditState(auditState, ff, ret, auth);
            return ResponseEntity.ok(retWithAuditState);
        }
    }

    @ApiOperation(value = "Get a list of services offered by a Provider.")
    @GetMapping(path = "services/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<? extends Service>> getServices(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(resourceBundleService.getResources(id, auth), HttpStatus.OK);
    }

    @ApiOperation(value = "Get a list of datasources offered by a Provider.")
    @GetMapping(path = "datasources/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<?>> getDatasources(@PathVariable("id") String id, @ApiIgnore Authentication auth) {
        return new ResponseEntity<>(datasourceBundleService.getResources(id, auth), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "byCatalogue/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isCatalogueAdmin(#auth,#id)")
    public ResponseEntity<Paging<ProviderBundle>> getProvidersByCatalogue(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth,
                                                                          @RequestParam(required = false) Set<String> status, @RequestParam(required = false) Set<String> templateStatus,
                                                                          @RequestParam(required = false) Set<String> auditState, @PathVariable String id) {
        Set<String> catalogueId = new LinkedHashSet<>();
        catalogueId.add(id);
        return getAllProviderBundles(allRequestParams, auth, status, templateStatus, auditState, catalogueId);
    }

    // Get a list of Providers in which the given user is admin.
    @GetMapping(path = "getServiceProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Provider>> getServiceProviders(@RequestParam("email") String email, @ApiIgnore Authentication auth) {
        List<Provider> providers = providerService.getServiceProviders(email, auth)
                .stream()
                .map(ProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(providers, HttpStatus.OK);
    }

    // Get a list of Providers in which you are admin.
    @GetMapping(path = "getMyServiceProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyServiceProviders(@ApiIgnore Authentication auth) {
        return new ResponseEntity<>(providerService.getMy(null, auth).getResults(), HttpStatus.OK);
    }

    // Get the pending services of the given Provider.
    @GetMapping(path = "services/pending/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Service>> getInactiveServices(@PathVariable("id") String id) {
        List<Service> ret = resourceBundleService.getInactiveResources(id).stream().map(ServiceBundle::getService).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @GetMapping(path = "datasources/pending/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Datasource>> getInactiveDatasources(@PathVariable("id") String id) {
        List<Datasource> ret = datasourceBundleService.getInactiveResources(id).stream().map(DatasourceBundle::getDatasource).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @GetMapping(path = "trainingResources/pending/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<TrainingResource>> getInactiveTrainingResources(@PathVariable("id") String id) {
        List<TrainingResource> ret = trainingResourceService.getInactiveResources(id).stream().map(TrainingResourceBundle::getTrainingResource).collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Get the rejected services of the given Provider.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "resources/rejected/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<ResourceBundle<?>>> getRejectedResources(@PathVariable("id") String providerId, @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                          @RequestParam String resourceType, @ApiIgnore Authentication auth) {
        allRequestParams.add("resource_organisation", providerId);
        allRequestParams.add("status", "rejected resource");
        allRequestParams.add("published", "false");
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        return ResponseEntity.ok(providerService.getRejectedResources(ff, resourceType, auth));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "datasources/rejected/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<DatasourceBundle>> getRejectedDatasources(@PathVariable("id") String providerId, @ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                     @ApiIgnore Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("status", "rejected resource");
        return ResponseEntity.ok(datasourceBundleService.getAll(ff, auth));
    }

    // Get all inactive Providers.
    @GetMapping(path = "inactive/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<Provider>> getInactive(@ApiIgnore Authentication auth) {
        List<Provider> ret = providerService.getInactive()
                .stream()
                .map(ProviderBundle::getProvider)
                .collect(Collectors.toList());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    // Accept/Reject a Provider.
    @PatchMapping(path = "verifyProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) String status, @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerService.verifyProvider(id, status, active, auth);
        logger.info("User '{}' updated Provider with name '{}' [status: {}] [active: {}]", auth, provider.getProvider().getName(), status, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Activate/Deactivate a Provider.
    @PatchMapping(path = "publish/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.providerIsActiveAndUserIsAdmin(#auth, #id)")
    public ResponseEntity<ProviderBundle> publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                  @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerService.publish(id, active, auth);
        logger.info("User '{}-{}' attempts to save Provider with id '{}' as '{}'", User.of(auth).getFullName(), User.of(auth).getEmail(), id, active);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    // Publish all Provider services.
    @PatchMapping(path = "publishServices", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<List<ServiceBundle>> publishServices(@RequestParam String id, @RequestParam Boolean active,
                                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle provider = providerService.get(catalogueName, id, auth);
        if (provider == null) {
            throw new ResourceException("Provider with id '" + id + "' does not exist.", HttpStatus.NOT_FOUND);
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("catalogue_id", catalogueName);
        List<ServiceBundle> services = resourceBundleService.getAll(ff, auth).getResults();
        for (ServiceBundle service : services) {
            service.setActive(active);
//            service.setStatus(status.getKey());
            Metadata metadata = service.getMetadata();
            metadata.setModifiedBy("system");
            metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            resourceBundleService.update(service, auth);
            logger.info("User '{}' published(updated) all Services of the Provider with name '{}'",
                    auth.getName(), provider.getProvider().getName());
        }
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    @PatchMapping(path = "publishDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<List<DatasourceBundle>> publishDatasources(@RequestParam String id, @RequestParam Boolean active,
                                                               @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ProviderBundle provider = providerService.get(catalogueName, id, auth);
        if (provider == null) {
            throw new ResourceException("Provider with id '" + id + "' does not exist.", HttpStatus.NOT_FOUND);
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("resource_organisation", id);
        ff.addFilter("catalogue_id", catalogueName);
        List<DatasourceBundle> datasources = datasourceBundleService.getAll(ff, auth).getResults();
        for (DatasourceBundle datasource : datasources) {
            datasource.setActive(active);
//            service.setStatus(status.getKey());
            Metadata metadata = datasource.getMetadata();
            metadata.setModifiedBy("system");
            metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
            datasourceBundleService.update(datasource, auth);
            logger.info("User '{}' published(updated) all Datasources of the Provider with name '{}'",
                    auth.getName(), provider.getProvider().getName());
        }
        return new ResponseEntity<>(datasources, HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String providerId, @ApiIgnore Authentication authentication) {
        return providerService.hasAdminAcceptedTerms(providerId, authentication);
    }

    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void adminAcceptedTerms(@RequestParam String providerId, @ApiIgnore Authentication authentication) {
        providerService.adminAcceptedTerms(providerId, authentication);
    }

    @GetMapping(path = "validateUrl", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean validateUrl(@RequestParam URL urlForValidation) throws Throwable {
        return providerService.validateUrl(urlForValidation);
    }

    @GetMapping(path = "requestProviderDeletion", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void requestProviderDeletion(@RequestParam String providerId, @ApiIgnore Authentication authentication) {
        providerService.requestProviderDeletion(providerId, authentication);
    }

    @DeleteMapping(path = "/delete/userInfo", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void deleteUserInfo(Authentication authentication) {
        providerService.deleteUserInfo(authentication);
    }

    // Get all modification details of a specific Provider based on id.
    @GetMapping(path = {"history/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<ResourceHistory>> history(@PathVariable String id,
                                                           @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        Paging<ResourceHistory> history = this.providerService.getHistory(id, catalogueId);
        return ResponseEntity.ok(history);
    }

    @PatchMapping(path = "auditProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> auditProvider(@PathVariable("id") String id, @RequestParam(required = false) String comment,
                                                        @RequestParam LoggingInfo.ActionType actionType, @ApiIgnore Authentication auth) {
        ProviderBundle provider = providerService.auditProvider(id, comment, actionType, auth);
        logger.info("User '{}-{}' audited Provider with name '{}' [actionType: {}]", User.of(auth).getFullName(), User.of(auth).getEmail(),
                provider.getProvider().getName(), actionType);
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "randomProviders", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Paging<ProviderBundle>> getRandomProviders(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String) allRequestParams.remove("quantity")) : 10);
        ff.setFilter(allRequestParams);
        ff.addFilter("status", "approved provider");
        ff.addFilter("published", false);
        Paging<ProviderBundle> providerBundlePaging = providerService.getRandomProviders(ff, auditingInterval, auth);
        return new ResponseEntity<>(providerBundlePaging, HttpStatus.OK);
    }

    // Get all modification details of a specific Provider based on id.
    @GetMapping(path = {"loggingInfoHistory/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<LoggingInfo>> loggingInfoHistory(@PathVariable String id,
                                                                  @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId) {
        Paging<LoggingInfo> loggingInfoHistory = this.providerService.getLoggingInfoHistory(id, catalogueId);
        return ResponseEntity.ok(loggingInfoHistory);
    }

    @ApiOperation(value = "Validates the Provider without actually changing the repository.")
    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody Provider provider) {
        ResponseEntity<Boolean> ret = ResponseEntity.ok(providerService.validate(new ProviderBundle(provider)) != null);
        logger.info("Validated Provider with name '{}' and id '{}'", provider.getName(), provider.getId());
        return ret;
    }

    // front-end use (Provider form)
    @GetMapping(path = {"providerIdToNameMap"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<eu.einfracentral.dto.Value>> providerIdToNameMap(String catalogueId) {
        List<eu.einfracentral.dto.Value> allProviders = new ArrayList<>();
        // fetch catalogueId related non-public Providers
        List<eu.einfracentral.dto.Value> catalogueRelatedProviders = providerService
                .getAll(createFacetFilter(catalogueId, false), securityService.getAdminAccess()).getResults()
                .stream().map(ProviderBundle::getProvider)
                .map(c -> new eu.einfracentral.dto.Value(c.getId(), c.getName()))
                .collect(Collectors.toList());
        // fetch non-catalogueId related public Providers
        List<eu.einfracentral.dto.Value> publicProviders = providerService
                .getAll(createFacetFilter(catalogueId, true), securityService.getAdminAccess()).getResults()
                .stream().map(ProviderBundle::getProvider)
                .filter(c -> !c.getCatalogueId().equals(catalogueId))
                .map(c -> new eu.einfracentral.dto.Value(c.getId(), c.getName()))
                .collect(Collectors.toList());

        allProviders.addAll(catalogueRelatedProviders);
        allProviders.addAll(publicProviders);

        return ResponseEntity.ok(allProviders);
    }

    private FacetFilter createFacetFilter(String catalogueId, boolean isPublic) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("status", "approved provider");
        ff.addFilter("active", true);
        if (isPublic) {
            ff.addFilter("published", true);
        } else {
            ff.addFilter("catalogue_id", catalogueId);
            ff.addFilter("published", false);
        }
        return ff;
    }

    @PutMapping(path = "changeCatalogue", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<ProviderBundle> changeCatalogue(@RequestParam String catalogueId, @RequestParam String providerId,
                                                          @RequestParam String newCatalogueId, @ApiIgnore Authentication authentication) {
        ProviderBundle providerBundle = migrationService.changeProviderCatalogue(providerId, catalogueId, newCatalogueId, authentication);
        return ResponseEntity.ok(providerBundle);
    }

    // Create a Public ProviderBundle if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicProvider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderBundle> createPublicProvider(@RequestBody ProviderBundle providerBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Provider from Provider '{}'-'{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), providerBundle.getId(), providerBundle.getProvider().getName(), providerBundle.getProvider().getCatalogueId());
        return ResponseEntity.ok(providerService.createPublicProvider(providerBundle, auth));
    }

    @ApiOperation(value = "Suspends a Provider")
    @PutMapping(path = "suspend", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ProviderBundle suspendProvider(@RequestParam String providerId, @RequestParam String catalogueId, @RequestParam boolean suspend, @ApiIgnore Authentication auth) {
        return providerService.suspend(providerId, catalogueId, suspend, auth);
    }
}
