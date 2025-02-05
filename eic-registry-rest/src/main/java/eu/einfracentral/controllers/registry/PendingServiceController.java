package eu.einfracentral.controllers.registry;

import eu.einfracentral.annotations.Browse;
import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.service.IdCreator;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import io.swagger.annotations.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping({"pendingService"})
@Api(description = "Operations for Pending Services", tags = {"pending-resource-controller"})
public class PendingServiceController extends ResourceController<ServiceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingServiceController.class);
    private final PendingResourceService<ServiceBundle> pendingServiceManager;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private GenericResourceService genericResourceService;
    private final IdCreator idCreator;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    PendingServiceController(PendingResourceService<ServiceBundle> pendingServiceManager,
                             ServiceBundleService<ServiceBundle> serviceBundleService,
                             ProviderService<ProviderBundle, Authentication> providerService,
                             GenericResourceService genericResourceService,
                             IdCreator idCreator) {
        super(pendingServiceManager);
        this.pendingServiceManager = pendingServiceManager;
        this.serviceBundleService = serviceBundleService;
        this.providerService = providerService;
        this.genericResourceService = genericResourceService;
        this.idCreator = idCreator;
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ServiceBundle> delete(@PathVariable("id") String id, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle service = pendingServiceManager.get(id);
        pendingServiceManager.delete(service);
        logger.info("User '{}' deleted Pending Resource '{}' with id: '{}'", auth.getName(), service.getService().getName(), service.getService().getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = "/service/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getService(@PathVariable String id) {
        return new ResponseEntity<>(pendingServiceManager.get(id).getService(), HttpStatus.OK);
    }

    @Browse
    @GetMapping(path = "/byProvider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth,#id,true)")
    public ResponseEntity<Paging<?>> getProviderPendingServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams,
                                                                @PathVariable String id, @ApiIgnore Authentication auth) {
        FacetFilter ff = serviceBundleService.createFacetFilterForFetchingServices(allRequestParams, catalogueName);
        ff.addFilter("resource_organisation", id);
        ff.setResourceType("pending_service");
        Paging<?> paging = genericResourceService.getResults(ff);
        return ResponseEntity.ok(paging);
    }

    @PostMapping(path = "/addResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Service> addService(@RequestBody Service service, @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = new ServiceBundle(service);
        return new ResponseEntity<>(pendingServiceManager.add(serviceBundle, auth).getService(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/updateResource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Service> updateService(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        ServiceBundle serviceBundle = pendingServiceManager.get(service.getId());
        serviceBundle.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(serviceBundle, auth).getService(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToPending(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToPending(serviceId, auth);
    }

    @PostMapping("/transform/resource")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void transformServiceToInfra(@RequestParam String serviceId, @ApiIgnore Authentication auth) {
        pendingServiceManager.transformToActive(serviceId, auth);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PostAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Service> temporarySavePending(@RequestBody Service service, @ApiIgnore Authentication auth) {
        ServiceBundle serviceBundle = new ServiceBundle();
        ServiceBundle toCreateId = new ServiceBundle();
        toCreateId.setService(service);
        service.setId(idCreator.createServiceId(toCreateId));
        try {
            serviceBundle = pendingServiceManager.get(service.getId());
            serviceBundle.setService(service);
            serviceBundle = pendingServiceManager.update(serviceBundle, auth);
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.debug("Pending Resource with id '{}' does not exist. Creating it...", service.getId());
            serviceBundle.setService(service);
            serviceBundle = pendingServiceManager.add(serviceBundle, auth);
        }
        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Service> temporarySaveService(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        pendingServiceManager.transformToPending(service.getId(), auth);
        ServiceBundle serviceBundle = pendingServiceManager.get(service.getId());
        serviceBundle.setService(service);
        return new ResponseEntity<>(pendingServiceManager.update(serviceBundle, auth).getService(), HttpStatus.OK);
    }

    @PutMapping(path = "/transform/resource", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Service> pendingToInfra(@RequestBody Service service, @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        if (service == null) {
            throw new ServiceException("Cannot add a null Resource");
        }
        ServiceBundle serviceBundle = null;

        try { // check if service already exists
            if (service.getId() == null || "".equals(service.getId())) { // if service id is not given, create it
                service.setId(idCreator.createServiceId(new ServiceBundle(service)));
            }
            serviceBundle = this.pendingServiceManager.get(service.getId());
        } catch (ResourceException | eu.einfracentral.exception.ResourceNotFoundException e) {
            // continue with the creation of the service
        }

        // check Provider's template status -> block transform if it's on 'pending' state
        String resourceOrganisation = service.getResourceOrganisation();
        ProviderBundle providerBundle = providerService.get(resourceOrganisation);
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ValidationException(String.format("There is already a Resource waiting to be approved for the Provider [%s]", resourceOrganisation));
        }

        if (serviceBundle == null) { // if existing Pending Service is null, create a new Active Service
            serviceBundle = serviceBundleService.addResource(new ServiceBundle(service), auth);
            logger.info("User '{}' added Resource:\n{}", auth.getName(), serviceBundle);
        } else { // else update Pending Service and transform it to Active Service
            if (serviceBundle.getService().getVersion() != null && serviceBundle.getService().getVersion().equals("")) {
                serviceBundle.getService().setVersion(null);
            }
            serviceBundle.setService(service); // important to keep other fields of ServiceBundle
            serviceBundle = pendingServiceManager.update(serviceBundle, auth);
            logger.info("User '{}' updated Pending Resource:\n{}", auth.getName(), serviceBundle);

            // transform to active
            serviceBundle = pendingServiceManager.transformToActive(serviceBundle.getId(), auth);
        }

        return new ResponseEntity<>(serviceBundle.getService(), HttpStatus.OK);
    }
}
