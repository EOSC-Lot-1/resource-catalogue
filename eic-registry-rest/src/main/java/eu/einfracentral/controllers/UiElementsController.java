package eu.einfracentral.controllers;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.RichService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.dto.UiService;
import eu.einfracentral.dto.Value;
import eu.einfracentral.registry.controller.InfraServiceController;
import eu.einfracentral.registry.controller.ServiceController;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.service.UiElementsService;
import eu.einfracentral.ui.Field;
import eu.einfracentral.ui.FieldGroup;
import eu.einfracentral.ui.GroupedFields;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("ui")
public class UiElementsController {

    private static final Logger logger = Logger.getLogger(UiElementsController.class);

    private final UiElementsService uiElementsService;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final InfraServiceController infraServiceController;

    @Autowired
    public UiElementsController(UiElementsService uiElementsService,
                                InfraServiceService<InfraService, InfraService> infraServiceService,
                                InfraServiceController infraServiceController) {
        this.uiElementsService = uiElementsService;
        this.infraServiceService = infraServiceService;
        this.infraServiceController = infraServiceController;
    }

    @GetMapping(value = "{className}/fields/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Field> createFields(@PathVariable("className") String name) throws ClassNotFoundException {
        return uiElementsService.createFields(name, null);
    }

    @GetMapping(value = "{className}/fields", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Field> getFields(@PathVariable("className") String name) {
        return uiElementsService.getFields();
    }

    @GetMapping(value = "form/model/flat", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GroupedFields<Field>> getFlatModel() {
        return uiElementsService.getFlatModel();
    }

    @GetMapping(value = "form/model", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GroupedFields<FieldGroup>> getModel() {
        return uiElementsService.getModel();
    }

    @GetMapping(path = "vocabulary/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Value> getByExtraVoc(@PathVariable("type") String vocabularyType, @RequestParam(name = "used", required = false) Boolean used) {
        return uiElementsService.getControlValues(vocabularyType, used);
    }

    @GetMapping(value = "vocabularies", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<Value>> getControlValuesByType() {
        return uiElementsService.getControlValuesMap();
    }


    // Services
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "services", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<UiService>> getAllUiServices(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        Paging<UiService> uiServicePaging = new Paging<>();
        ResponseEntity<Paging<InfraService>> servicesResponse = infraServiceController.getAllActiveLatest(allRequestParams, authentication);
        if (servicesResponse.hasBody()) {
            Paging<InfraService> services = servicesResponse.getBody();
            List<UiService> uiServiceList = services.getResults()
                    .stream()
                    .map(uiElementsService::createUiService)
                    .collect(Collectors.toList());
            uiServicePaging = new Paging<>(services, uiServiceList);
        }
        return new ResponseEntity<>(uiServicePaging, servicesResponse.getStatusCode());
    }

    @GetMapping(path = "services/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public UiService getUiService(@PathVariable("id") String id) {
        return uiElementsService.createUiService(infraServiceService.get(id));
    }

    @PostMapping(path = "services", produces = {MediaType.APPLICATION_JSON_VALUE})
    public UiService addDynamic(@RequestBody UiService service, Authentication authentication) {
        logger.info(service);
        InfraService infra = uiElementsService.createService(service);
        infra = infraServiceService.addService(infra, authentication);
        return uiElementsService.createUiService(infra);
    }

    @PutMapping(path = "services", produces = {MediaType.APPLICATION_JSON_VALUE})
    public UiService putDynamic(@RequestBody UiService service, @RequestParam(required = false) String comment, Authentication authentication) throws ResourceNotFoundException {
        logger.info(service);
        InfraService infra = uiElementsService.createService(service);
        if (infra.getId() == null) {
            return addDynamic(service, authentication);
        }
        InfraService previous = infraServiceService.get(infra.getId());
        previous.setService(infra.getService());
        previous.setExtras(infra.getExtras());
        infra = infraServiceService.updateService(previous, comment, authentication);

        return uiElementsService.createUiService(infra);
    }

    // Get a list of UiServices based on a set of ids.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "services/ids/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<UiService>> getSomeServices(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        List<InfraService> services = Arrays.stream(ids).map(infraServiceService::get).collect(Collectors.toList());
        List<UiService> uiServices = services
                .stream()
                .map(uiElementsService::createUiService)
                .collect(Collectors.toList());
        return ResponseEntity.ok(uiServices);
    }

    // Get a list of UiService snippets based on a set of ids.
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
    @GetMapping(path = "services/snippets/ids/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Map<String, Object>>> getSomeServicesSnippets(@PathVariable String[] ids, @ApiIgnore Authentication auth) {
        List<InfraService> services = Arrays.stream(ids).map(infraServiceService::get).collect(Collectors.toList());
        List<Map<String, Object>> snippets = services
                .stream()
                .map(uiElementsService::createServiceSnippet)
                .collect(Collectors.toList());
        return ResponseEntity.ok(snippets);
    }

    // Snippets
    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping(path = "services/snippets", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Map<String, Object>>> getAllSnippets(@ApiIgnore @RequestParam MultiValueMap<String, Object> allRequestParams, @ApiIgnore Authentication authentication) {
        Paging<Map<String, Object>> snippets = new Paging<>();
        ResponseEntity<Paging<InfraService>> servicesResponse = infraServiceController.getAllActiveLatest(allRequestParams, authentication);
        if (servicesResponse.hasBody()) {
            Paging<InfraService> services = servicesResponse.getBody();
            List<Map<String, Object>> snippetsList = services.getResults()
                    .stream()
                    .parallel()
                    .map(uiElementsService::createServiceSnippet)
                    .collect(Collectors.toList());
            snippets = new Paging<>(services, snippetsList);
        }
        return new ResponseEntity<>(snippets, servicesResponse.getStatusCode());
    }

    @GetMapping(path = "services/snippets/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, Object> getServiceSnippet(@PathVariable("id") String id) {
        return uiElementsService.createServiceSnippet(infraServiceService.get(id));
    }

    @GetMapping(path = "services/by/extra/{vocabulary}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, List<UiService>> getUiServicesByExtraVoc(@PathVariable("vocabulary") String vocabularyType, @RequestParam(name = "value", required = false) String value) {
        return uiElementsService.getUiServicesByExtraVoc(vocabularyType, value);
    }

    @GetMapping(path = "services/snippets/by/extra/{vocabulary}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, List<Map<String, Object>>> getServicesSnippetsByExtraVoc(@PathVariable("vocabulary") String vocabularyType, @RequestParam(name = "value", required = false) String value) {
        return uiElementsService.getServicesSnippetsByExtraVoc(vocabularyType, value);
    }

    @GetMapping(path = "services/names/by/indexed/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, List<Value>> getServicesSnippetsByIndexedVoc(@PathVariable("field") String field, @RequestParam(name = "vocabularyType") String vocabularyType) {
        return uiElementsService.getServiceNamesByIndexedVoc(field, vocabularyType);
    }
}
