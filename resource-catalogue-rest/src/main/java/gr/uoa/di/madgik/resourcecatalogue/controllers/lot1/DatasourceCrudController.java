package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.EnumVariableType;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;
import gr.uoa.di.madgik.resourcecatalogue.dto.ProcessInstanceRequestDto;
import gr.uoa.di.madgik.resourcecatalogue.dto.ProcessInstanceRequestVariableDto;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.OpenAIREDatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Profile("crud")
@RestController
@RequestMapping(path = "datasources")
@Tag(name = "datasources")
public class DatasourceCrudController extends ResourceCrudController<DatasourceBundle> {

    private static final Logger logger = LogManager.getLogger(DatasourceCrudController.class);
    private final DatasourceService datasourceService;
    private final OpenAIREDatasourceService openAIREDatasourceService;
    private final RabbitTemplate      rabbitTemplate;
    private final ObjectMapper        objectMapper;
    private final String              transactionExchange;
    private final String              transactionRoutingKey;

    public DatasourceCrudController(DatasourceService datasourceService,
                                    OpenAIREDatasourceService openAIREDatasourceService,
                                    RabbitTemplate rabbitTemplate,
                            		ObjectMapper objectMapper, 
                            		@Value("${eosc.amqp.exchange.transactions.name:workflows}")
                            		String              transactionExchange,
                            		@Value("${eosc.amqp.exchange.transactions.routing-key:}")
                            		String              transactionRoutingKey) {
        super(datasourceService);
        this.datasourceService = datasourceService;
        this.openAIREDatasourceService = openAIREDatasourceService;
		this.rabbitTemplate = rabbitTemplate;
	    this.objectMapper = objectMapper;
	    this.transactionExchange = transactionExchange;
	    this.transactionRoutingKey = transactionRoutingKey;
    }

    @Operation(summary = "Returns the Datasource of the given Service of the given Catalogue.")
    @GetMapping(path = "/byService/{serviceId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getDatasourceByServiceId(@PathVariable("serviceId") String serviceId,
                                                               @RequestParam(defaultValue = "${catalogue.id}", name = "catalogue_id") String catalogueId,
                                                               @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("service_id", serviceId);
        List<DatasourceBundle> allDatasources = datasourceService.getAll(ff, auth).getResults();
        if (!allDatasources.isEmpty()) {
            return new ResponseEntity<>(allDatasources.get(0).getDatasource(), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    // OpenAIRE related methods
    @GetMapping(path = "/getOpenAIREDatasourceById", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Datasource> getOpenAIREDatasourceById(@RequestParam String datasourceId) throws IOException {
        return ResponseEntity.ok(openAIREDatasourceService.get(datasourceId));
    }

    @GetMapping(path = "isDatasourceRegisteredOnOpenAIRE/{id}")
    public boolean isDatasourceRegisteredOnOpenAIRE(@PathVariable("id") String id) {
        return datasourceService.isDatasourceRegisteredOnOpenAIRE(id);
    }

    @Browse
    @GetMapping(path = "/getAllOpenAIREDatasources", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Paging<Datasource>> getAllOpenAIREDatasources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) throws IOException {
        FacetFilter ff = FacetFilter.from(allRequestParams);
        Map<Integer, List<Datasource>> datasourceMap = openAIREDatasourceService.getAll(ff);
        Paging<Datasource> datasourcePaging = new Paging<>();
        datasourcePaging.setTotal(datasourceMap.keySet().iterator().next());
        datasourcePaging.setFrom(ff.getFrom());
        datasourcePaging.setTo(ff.getFrom() + ff.getQuantity());
        datasourcePaging.setResults(datasourceMap.get(datasourcePaging.getTotal()));
        return ResponseEntity.ok(new Paging<>(datasourcePaging.getTotal(), datasourcePaging.getFrom(),
                datasourcePaging.getTo(), datasourcePaging.getResults(), datasourcePaging.getFacets()));
    }

    @GetMapping(path = "isMetricsValid/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public OpenAIREMetrics getOpenaireMetrics(@PathVariable("id") String id) {
        return openAIREDatasourceService.getMetrics(id);
    }

    @PostMapping(path = "/bulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<DatasourceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        datasourceService.addBulk(bundles, auth);
    }
    
    @PostMapping(path = "/publish/", produces = MediaType.APPLICATION_JSON_VALUE)
    public void publish(@RequestParam("id") @Parameter(allowReserved = true) String id, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException, IOException, TimeoutException {
    	List<ProcessInstanceRequestVariableDto> variables = new ArrayList<ProcessInstanceRequestVariableDto>();
    	variables.add(new ProcessInstanceRequestVariableDto("pid", id, EnumVariableType.STRING));
    	ProcessInstanceRequestDto instanceRequest = new ProcessInstanceRequestDto(null, null, "datasource-publish", variables);
        this.sendMessage(instanceRequest);
    }
    
    private void sendMessage(ProcessInstanceRequestDto request) throws JsonProcessingException {
        final var payload = objectMapper.writeValueAsBytes(request);
        final var message = new Message(payload);
        this.rabbitTemplate.send(this.transactionExchange,this.transactionRoutingKey ,message);
    }
}
