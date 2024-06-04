package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.PendingResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("pendingProvider")
@Tag(name = "pending provider")
public class PendingProviderController extends ResourceController<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PendingProviderController.class);

    private final PendingResourceService<ProviderBundle> pendingProviderService;
    private final ProviderService<ProviderBundle, Authentication> providerManager;
    private final IdCreator idCreator;

    PendingProviderController(PendingResourceService<ProviderBundle> pendingProviderService,
                              ProviderService<ProviderBundle, Authentication> providerManager,
                              IdCreator idCreator) {
        super(pendingProviderService);
        this.pendingProviderService = pendingProviderService;
        this.providerManager = providerManager;
        this.idCreator = idCreator;
    }

    @GetMapping(path = "/provider/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Provider> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(pendingProviderService.get(id).getProvider(), HttpStatus.OK);
    }

    @GetMapping(path = "/id", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getIdFromOriginalId(@RequestParam("originalId") String originalId) {
        return new ResponseEntity<>(pendingProviderService.getId(originalId), HttpStatus.OK);
    }

    @GetMapping(path = "/id/mappings", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, String>> getIdFromOriginalId() {
        return new ResponseEntity<>(pendingProviderService.getIdOriginalIdMap(), HttpStatus.OK);
    }

    @PostMapping("/transform/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void transformProviderToPending(@RequestParam String providerId, @Parameter(hidden = true) Authentication auth) {
        pendingProviderService.transformToPending(providerId, auth);
    }

    @PostMapping("/transform/active")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public void transformProviderToActive(@RequestParam String providerId, @Parameter(hidden = true) Authentication auth) {
        pendingProviderService.transformToActive(providerId, auth);
    }

    @PutMapping(path = "/transform/active", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> updateAndPublish(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ProviderBundle providerBundle = pendingProviderService.get(provider.getId());
        providerBundle.setProvider(provider);

        // validate the Provider and update afterward ( update may change provider id and all of its services ids )
        providerManager.validate(providerBundle);

        update(providerBundle, auth);

        // transform to active
        providerBundle = pendingProviderService.transformToActive(providerBundle.getId(), auth);

        return new ResponseEntity<>(providerBundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/pending", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Provider> temporarySavePending(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) {
        ProviderBundle bundle = new ProviderBundle();
        provider.setId(idCreator.generate("pro"));
        try {
            bundle = pendingProviderService.get(provider.getId());
            bundle.setProvider(provider);
            bundle = pendingProviderService.update(bundle, auth);
        } catch (ResourceException | ResourceNotFoundException e) {
            logger.debug("Pending Provider with id '{}' does not exist. Creating it...", provider.getId());
            bundle.setProvider(provider);
            bundle = pendingProviderService.add(bundle, auth);
        }
        logger.info("User '{}' saved a Draft Provider with id '{}'", User.of(auth).getEmail(), provider.getId());
        return new ResponseEntity<>(bundle.getProvider(), HttpStatus.OK);
    }

    @PutMapping(path = "/provider", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT') or @securityService.isProviderAdmin(#auth, #provider.id)")
    public ResponseEntity<Provider> temporarySaveProvider(@RequestBody Provider provider, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        pendingProviderService.transformToPending(provider.getId(), auth);
        ProviderBundle bundle = pendingProviderService.get(provider.getId());
        bundle.setProvider(provider);
        return new ResponseEntity<>(pendingProviderService.update(bundle, auth).getProvider(), HttpStatus.OK);
    }

    // Get a list of Providers in which you are admin.
    @GetMapping(path = "getMyPendingProviders", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ProviderBundle>> getMyPendingProviders(@Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(pendingProviderService.getMy(auth), HttpStatus.OK);
    }

    @GetMapping(path = "hasAdminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public boolean hasAdminAcceptedTerms(@RequestParam String providerId, @Parameter(hidden = true) Authentication authentication) {
        return pendingProviderService.hasAdminAcceptedTerms(providerId, authentication);
    }

    @PutMapping(path = "adminAcceptedTerms", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public void adminAcceptedTerms(@RequestParam String providerId, @Parameter(hidden = true) Authentication authentication) {
        pendingProviderService.adminAcceptedTerms(providerId, authentication);
    }

}
