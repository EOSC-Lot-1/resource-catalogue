package eu.einfracentral.controllers.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.*;
import eu.einfracentral.dto.MonitoringStatus;
import eu.einfracentral.dto.ServiceType;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.HelpdeskService;
import eu.einfracentral.registry.service.MonitoringService;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.utils.CreateArgoGrnetHttpRequest;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.validators.HelpdeskValidator;
import eu.einfracentral.validators.MonitoringValidator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("service-extensions")
@Api(value = "Get information about Service Helpdesk and Monitoring")
public class ServiceExtensionsController {

    private static final Logger logger = LogManager.getLogger(ServiceExtensionsController.class);
    private final HelpdeskService<HelpdeskBundle, Authentication> helpdeskService;
    private final MonitoringService<MonitoringBundle, Authentication> monitoringService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    @Value("${argo.grnet.monitoring.availability}")
    private String monitoringAvailability;
    @Value("${argo.grnet.monitoring.status}")
    private String monitoringStatus;
    @Value("${argo.grnet.monitoring.token}")
    private String monitoringToken;

    @InitBinder("helpdesk")
    protected void initHelpdeskBinder(WebDataBinder binder) {
        binder.addValidators(new HelpdeskValidator());
    }

    @InitBinder("monitoring")
    protected void initMonitoringBinder(WebDataBinder binder) {
        binder.addValidators(new MonitoringValidator());
    }

    @Autowired
    ServiceExtensionsController(HelpdeskService<HelpdeskBundle, Authentication> helpdeskService,
                                MonitoringService<MonitoringBundle, Authentication> monitoringService,
                                ServiceBundleService<ServiceBundle> serviceBundleService) {
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.serviceBundleService = serviceBundleService;
    }

    //SECTION: HELPDESK
    @ApiOperation(value = "Returns the Helpdesk with the given id.")
    @GetMapping(path = "/helpdesk/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Helpdesk> getHelpdesk(@PathVariable("id") String id) {
        Helpdesk helpdesk = helpdeskService.get(id).getHelpdesk();
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    @GetMapping(path = "/helpdesk/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<HelpdeskBundle> getHelpdeskBundle(@PathVariable("id") String id) {
        HelpdeskBundle helpdesk = helpdeskService.get(id);
        return new ResponseEntity<>(helpdesk, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Helpdesk of the given Service of the given Catalogue.")
    @GetMapping(path = "/helpdesk/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Helpdesk> getHelpdeskByServiceId(@PathVariable("serviceId") String serviceId,
                                                           @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                           @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<HelpdeskBundle> allHelpdesks = helpdeskService.getAll(ff, auth).getResults();
        for (HelpdeskBundle helpdesk : allHelpdesks) {
            if (helpdesk.getCatalogueId().equals(catalogueId) && (helpdesk.getHelpdesk().getServiceId().equals(serviceId)
                    || (catalogueId + '.' + helpdesk.getHelpdesk().getServiceId()).equals(serviceId))) {
                return new ResponseEntity<>(helpdesk.getHelpdesk(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Helpdesks based on a set of filters or get a list of all Helpdesks in the Catalogue.")
    @Browse
    @GetMapping(path = "/helpdesk/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Helpdesk>> getAllHelpdesks(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                            @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueIds,
                                                            @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        List<Helpdesk> helpdeskList = new LinkedList<>();
        Paging<HelpdeskBundle> helpdeskBundlePaging = helpdeskService.getAll(ff, auth);
        for (HelpdeskBundle helpdeskBundle : helpdeskBundlePaging.getResults()) {
            helpdeskList.add(helpdeskBundle.getHelpdesk());
        }
        Paging<Helpdesk> helpdeskPaging = new Paging<>(helpdeskBundlePaging.getTotal(), helpdeskBundlePaging.getFrom(),
                helpdeskBundlePaging.getTo(), helpdeskList, helpdeskBundlePaging.getFacets());
        return new ResponseEntity<>(helpdeskPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Helpdesk.")
    @PostMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Helpdesk> addHelpdesk(@Valid @RequestBody Helpdesk helpdesk,
                                                @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                @RequestParam String resourceType,
                                                @ApiIgnore Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.add(new HelpdeskBundle(helpdesk, catalogueId), resourceType, auth);
        logger.info("User '{}' added the Helpdesk with id '{}'", auth.getName(), helpdesk.getId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Helpdesk with the given id.")
    @PutMapping(path = "/helpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #helpdesk.serviceId, #catalogueId)")
    public ResponseEntity<Helpdesk> updateHelpdesk(@Valid @RequestBody Helpdesk helpdesk,
                                                   @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(helpdesk.getId());
        helpdeskBundle.setHelpdesk(helpdesk);
        helpdeskBundle = helpdeskService.update(helpdeskBundle, auth);
        logger.info("User '{}' updated the Helpdesk with id '{}'", auth.getName(), helpdesk.getId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    // Deletes the Helpdesk with the specific ID.
    @DeleteMapping(path = "/helpdesk/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Helpdesk> deleteHelpdeskById(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(id);
        if (helpdeskBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Helpdesk: {} of the Catalogue: {}", helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        // delete Helpdesk
        helpdeskService.delete(helpdeskBundle);
        logger.info("User '{}' deleted the Helpdesk with id '{}' of the Catalogue '{}'", auth.getName(), helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    // Deletes the Helpdesk of the specific Service of the specific Catalogue.
    @ApiOperation(value = "Deletes the Helpdesk of the specific Service of the specific Catalogue.")
    @DeleteMapping(path = "/helpdesk/{catalogueId}/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId, #catalogueId)")
    public ResponseEntity<Helpdesk> deleteHelpdesk(@PathVariable("catalogueId") String catalogueId,
                                                   @PathVariable("serviceId") String serviceId,
                                                   @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Helpdesk helpdesk = getHelpdeskByServiceId(serviceId, catalogueId, auth).getBody();
        assert helpdesk != null;
        HelpdeskBundle helpdeskBundle = helpdeskService.get(helpdesk.getId());
        if (helpdeskBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Helpdesk: {} of the Catalogue: {}", helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        // delete Helpdesk
        helpdeskService.delete(helpdeskBundle);
        logger.info("User '{}' deleted the Helpdesk with id '{}' of the Catalogue '{}'", auth.getName(), helpdeskBundle.getHelpdesk().getId(), helpdeskBundle.getCatalogueId());
        return new ResponseEntity<>(helpdeskBundle.getHelpdesk(), HttpStatus.OK);
    }

    // Create a Public HelpdeskBundle if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicHelpdesk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<HelpdeskBundle> createPublicHelpdesk(@RequestBody HelpdeskBundle helpdeskBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Helpdesk from Helpdesk '{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), helpdeskBundle.getId(), helpdeskBundle.getCatalogueId());
        return ResponseEntity.ok(helpdeskService.createPublicResource(helpdeskBundle, auth));
    }

    @ApiIgnore
    @PostMapping(path = "createPublicHelpdesks", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void createPublicHelpdesks(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<HelpdeskBundle> allHelpdesks = helpdeskService.getAll(ff, auth).getResults();
        for (HelpdeskBundle helpdeskBundle : allHelpdesks) {
            try {
                helpdeskService.createPublicResource(helpdeskBundle, auth);
            } catch (ResourceException e) {
                logger.info("Helpdesk with ID {} is already registered as Public", helpdeskBundle.getId());
            }
        }
    }


    //SECTION: MONITORING
    @ApiOperation(value = "Returns the Monitoring with the given id.")
    @GetMapping(path = "/monitoring/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Monitoring> getMonitoring(@PathVariable("id") String id) {
        Monitoring monitoring = monitoringService.get(id).getMonitoring();
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }

    @GetMapping(path = "/monitoring/bundle/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<MonitoringBundle> getMonitoringBundle(@PathVariable("id") String id) {
        MonitoringBundle monitoring = monitoringService.get(id);
        return new ResponseEntity<>(monitoring, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the Monitoring of the given Service of the given Catalogue.")
    @GetMapping(path = "/monitoring/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Monitoring> getMonitoringByServiceId(@PathVariable("serviceId") String serviceId,
                                                               @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                               @ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<MonitoringBundle> allMonitorings = monitoringService.getAll(ff, auth).getResults();
        for (MonitoringBundle monitoring : allMonitorings) {
            if (monitoring.getCatalogueId().equals(catalogueId) && (monitoring.getMonitoring().getServiceId().equals(serviceId)
                    || (catalogueId + '.' + monitoring.getMonitoring().getServiceId()).equals(serviceId))) {
                return new ResponseEntity<>(monitoring.getMonitoring(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @ApiOperation(value = "Filter a list of Monitorings based on a set of filters or get a list of all Monitorings in the Catalogue.")
    @Browse
    @GetMapping(path = "monitoring/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<Monitoring>> getAllMonitorings(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                                                @RequestParam(defaultValue = "all", name = "catalogue_id") String catalogueIds,
                                                                @ApiIgnore Authentication auth) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        List<Monitoring> monitoringList = new LinkedList<>();
        Paging<MonitoringBundle> monitoringBundlePaging = monitoringService.getAll(ff, auth);
        for (MonitoringBundle monitoringBundle : monitoringBundlePaging.getResults()) {
            monitoringList.add(monitoringBundle.getMonitoring());
        }
        Paging<Monitoring> monitoringPaging = new Paging<>(monitoringBundlePaging.getTotal(), monitoringBundlePaging.getFrom(),
                monitoringBundlePaging.getTo(), monitoringList, monitoringBundlePaging.getFacets());
        return new ResponseEntity<>(monitoringPaging, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns all the available Monitoring serviceTypes")
    @GetMapping(path = "/monitoring/serviceTypes", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<List<ServiceType>> getAvailableServiceTypes() {
        return new ResponseEntity<>(monitoringService.getAvailableServiceTypes(), HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new Monitoring.")
    @PostMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Monitoring> addMonitoring(@Valid @RequestBody Monitoring monitoring,
                                                    @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                    @RequestParam String resourceType,
                                                    @ApiIgnore Authentication auth) {
        MonitoringBundle monitoringBundle = monitoringService.add(new MonitoringBundle(monitoring, catalogueId), resourceType, auth);
        logger.info("User '{}' added the Monitoring with id '{}'", auth.getName(), monitoring.getId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the Monitoring with the given id.")
    @PutMapping(path = "/monitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #monitoring.serviceId, #catalogueId)")
    public ResponseEntity<Monitoring> updateMonitoring(@Valid @RequestBody Monitoring monitoring,
                                                       @RequestParam(defaultValue = "${project.catalogue.name}", name = "catalogue_id") String catalogueId,
                                                       @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        MonitoringBundle monitoringBundle = monitoringService.get(monitoring.getId());
        monitoringBundle.setMonitoring(monitoring);
        monitoringBundle = monitoringService.update(monitoringBundle, auth);
        logger.info("User '{}' updated the Monitoring with id '{}'", auth.getName(), monitoring.getId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }

    // Deletes the Helpdesk of the given Service ID of the given Catalogue.
    @DeleteMapping(path = "/monitoring/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<Monitoring> deleteMonitoringById(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        MonitoringBundle monitoringBundle = monitoringService.get(id);
        if (monitoringBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Monitoring: {} of the Catalogue: {}", monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        // delete Monitoring
        monitoringService.delete(monitoringBundle);
        logger.info("User '{}' deleted the Monitoring with id '{}' of the Catalogue '{}'", auth.getName(), monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }

    // Deletes the Monitoring of the specific Service of the specific Catalogue.
    @ApiOperation(value = "Deletes the Monitoring of the specific Service of the specific Catalogue.")
    @DeleteMapping(path = "/monitoring/{catalogueId}/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isResourceProviderAdmin(#auth, #serviceId, #catalogueId)")
    public ResponseEntity<Monitoring> deleteMonitoring(@PathVariable("catalogueId") String catalogueId,
                                                       @PathVariable("serviceId") String serviceId,
                                                       @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        Monitoring monitoring = getMonitoringByServiceId(serviceId, catalogueId, auth).getBody();
        assert monitoring != null;
        MonitoringBundle monitoringBundle = monitoringService.get(monitoring.getId());
        if (monitoringBundle == null) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }
        logger.info("Deleting Monitoring: {} of the Catalogue: {}", monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        // delete Monitoring
        monitoringService.delete(monitoringBundle);
        logger.info("User '{}' deleted the Monitoring with id '{}' of the Catalogue '{}'", auth.getName(), monitoringBundle.getMonitoring().getId(), monitoringBundle.getCatalogueId());
        return new ResponseEntity<>(monitoringBundle.getMonitoring(), HttpStatus.OK);
    }


    // Argo GRNET Monitoring Status API calls
    @GetMapping(path = "/monitoring/monitoringAvailability/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<MonitoringStatus> getMonitoringAvailability(@PathVariable String serviceId, @RequestParam String start_time, @RequestParam String end_time) {
        String url = monitoringAvailability + serviceId + "?start_time=" + start_time + "&end_time=" + end_time;
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray results = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).getAsJsonObject().get("results").getAsJsonArray();
            serviceMonitoringStatuses = monitoringService.createMonitoringAvailabilityObject(results);
            return serviceMonitoringStatuses;
        }
        return null;
    }

    @GetMapping(path = "/monitoring/monitoringStatus/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<MonitoringStatus> getMonitoringStatus(@PathVariable String serviceId, @RequestParam(defaultValue = "false") Boolean allStatuses) {
        String url = monitoringStatus + serviceId;
        if (allStatuses != null) {
            if (allStatuses) {
                url += "?view=details";
            }
        }
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray statuses = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).getAsJsonObject().get("statuses").getAsJsonArray();
            serviceMonitoringStatuses = monitoringService.createMonitoringStatusObject(statuses);
            return serviceMonitoringStatuses;
        }
        return null;
    }

    @GetMapping(path = "/monitoring/monitoringStatusOnSpecificPeriod/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public List<MonitoringStatus> getMonitoringStatusOnSpecificPeriod(@PathVariable String serviceId, @RequestParam String from, @RequestParam String to) {
        OffsetDateTime odtFrom = OffsetDateTime.parse(from + "T00:00:01Z");
        OffsetDateTime odtTo = OffsetDateTime.parse(to + "T23:59:59Z");
        String url = monitoringStatus + serviceId + "?start_time=" + odtFrom + "&end_time=" + odtTo;
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray statuses = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).getAsJsonObject().get("statuses").getAsJsonArray();
            serviceMonitoringStatuses = monitoringService.createMonitoringStatusObject(statuses);
            return serviceMonitoringStatuses;
        }
        return null;
    }

    @GetMapping(path = "/monitoring/monitoringStatus/all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Map<String, String> getMonitoringStatusForAllServices() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        List<ServiceBundle> allServices = serviceBundleService.getAll(ff, null).getResults();
        Map<String, String> serviceStatusMap = new HashMap<>();
        for (ServiceBundle serviceBundle : allServices) {
            serviceStatusMap.put(serviceBundle.getId(), getServiceMonitoringStatusValue(serviceBundle.getId()));
        }
        return serviceStatusMap;
    }

    private String getServiceMonitoringStatusValue(String serviceId) {
        try {
            return getMonitoringStatus(serviceId, false).get(0).getValue();
        } catch (NullPointerException e) {
            return "";
        }
    }

    // Create a Public MonitoringBundle if something went bad during its creation
    @ApiIgnore
    @PostMapping(path = "createPublicMonitoring", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MonitoringBundle> createPublicMonitoring(@RequestBody MonitoringBundle monitoringBundle, @ApiIgnore Authentication auth) {
        logger.info("User '{}-{}' attempts to create a Public Monitoring from Monitoring '{}' of the '{}' Catalogue", User.of(auth).getFullName(),
                User.of(auth).getEmail(), monitoringBundle.getId(), monitoringBundle.getCatalogueId());
        return ResponseEntity.ok(monitoringService.createPublicResource(monitoringBundle, auth));
    }

    @ApiIgnore
    @PostMapping(path = "createPublicMonitorings", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void createPublicMonitorings(@ApiIgnore Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        List<MonitoringBundle> allMonitorings = monitoringService.getAll(ff, auth).getResults();
        for (MonitoringBundle monitoringBundle : allMonitorings) {
            try {
                monitoringService.createPublicResource(monitoringBundle, auth);
            } catch (ResourceException e) {
                logger.info("Monitoring with ID {} is already registered as Public", monitoringBundle.getId());
            }
        }
    }

}
