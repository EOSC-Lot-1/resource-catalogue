package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ToolService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "tools")
@Tag(name = "tools")
public class ToolCrudController extends ResourceCrudController<ToolBundle> {

    private static final Logger logger = LogManager.getLogger(ToolCrudController.class.getName());
    private final ToolService toolService;
    private final DraftResourceService<ToolBundle> draftToolService;

    ToolCrudController(ToolService toolService,
                                   DraftResourceService<ToolBundle> draftToolService) {
        super(toolService);
        this.toolService = toolService;
        this.draftToolService = draftToolService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ToolBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        toolService.addBulk(bundles, auth);
    }

    @Override
    @PostMapping()
    public ResponseEntity<ToolBundle> add(@RequestBody ToolBundle toolBundle,
                                                      @Parameter(hidden = true) Authentication auth) {
        if (toolBundle.isDraft()) {
            return new ResponseEntity<>(draftToolService.save(toolBundle), HttpStatus.CREATED);
        }
        return super.add(toolBundle, auth);
    }
}
