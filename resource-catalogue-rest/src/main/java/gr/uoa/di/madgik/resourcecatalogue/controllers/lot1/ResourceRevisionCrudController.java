package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceRevisionBundle;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceRevisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Profile("crud")
@RestController
@RequestMapping(path = "resource-revisions")
@Tag(name = "resource-revisions")
public class ResourceRevisionCrudController extends ResourceCrudController<ResourceRevisionBundle> {

    private static final Logger logger = LogManager.getLogger(ResourceRevisionCrudController.class.getName());
    private final ResourceRevisionService resourceRevisionService;

    ResourceRevisionCrudController(ResourceRevisionService resourceRevisionService) {
        super(resourceRevisionService);
        this.resourceRevisionService = resourceRevisionService;
    }

   
}
