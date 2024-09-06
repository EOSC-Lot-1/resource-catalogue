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
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.ToolBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ToolService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Profile("crud")
@RestController
@RequestMapping(path = "tools")
@Tag(name = "tools")
public class ToolCrudController extends ResourceCrudController<ToolBundle> {

    private static final Logger logger = LogManager.getLogger(ToolCrudController.class.getName());
    private final ToolService toolService;

    ToolCrudController(ToolService toolService) {
        super(toolService);
        this.toolService = toolService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ToolBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        toolService.addBulk(bundles, auth);
    }
    
    @Operation(summary = "Get tools by status/date")
    @Browse
    @GetMapping(path = "security-evaluations", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ToolBundle>> getToolsAccordingToDate(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams,
                                               @Parameter(description = "Before date (format yyyy-MM-dd)", example = "2023-01-01") 
                                               @RequestParam String date) {
    	String status = (String) allRequestParams.get("status");
    	allRequestParams.remove("date");
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        List<ToolBundle> tools = toolService.getAll(ff).getResults();
        List<ToolBundle> filteredTools = toolService.getToolsByDateStatus(date, status, tools); // Use the return value
        return new ResponseEntity<>(filteredTools, HttpStatus.OK);
    }
}
