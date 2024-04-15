package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplate;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ConfigurationTemplateService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("configurationTemplate")
//@Tag(value = "Operations for Configuration Templates")
public class ConfigurationTemplateController {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateController.class);
    private final ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService;

    public ConfigurationTemplateController(ConfigurationTemplateService<ConfigurationTemplateBundle> configurationTemplateService) {
        this.configurationTemplateService = configurationTemplateService;
    }

    //    @Operation(description = "Create a new ConfigurationTemplate.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ConfigurationTemplate> addConfigurationTemplate(@RequestBody ConfigurationTemplate configurationTemplate,
                                                                          @Parameter(hidden = true) Authentication auth) {
        ConfigurationTemplateBundle configurationTemplateBundle = configurationTemplateService.addConfigurationTemplate(new ConfigurationTemplateBundle(configurationTemplate), auth);
        logger.info("User '{}' added the Configuration Template Instance with id '{}'", auth.getName(), configurationTemplate.getId());
        return new ResponseEntity<>(configurationTemplateBundle.getConfigurationTemplate(), HttpStatus.CREATED);
    }
}
