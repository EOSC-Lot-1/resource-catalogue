package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.EnumVariableType;
import gr.uoa.di.madgik.resourcecatalogue.dto.ProcessInstanceRequestDto;
import gr.uoa.di.madgik.resourcecatalogue.dto.ProcessInstanceRequestVariableDto;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Profile("crud")
@RestController
@RequestMapping(path = "training-resources")
@Tag(name = "training resources")
public class TrainingResourceCrudController extends ResourceCrudController<TrainingResourceBundle> {

    private static final Logger logger = LogManager.getLogger(TrainingResourceCrudController.class.getName());
    private final TrainingResourceService trainingResourceService;
    private final RabbitTemplate      rabbitTemplate;
    private final ObjectMapper        objectMapper;
    private final String              transactionExchange;
    private final String              transactionRoutingKey;

    TrainingResourceCrudController(TrainingResourceService trainingResourceService, RabbitTemplate rabbitTemplate,
    		ObjectMapper objectMapper, 
    		@Value("${eosc.amqp.exchange.transactions.name:workflows}")
    		String              transactionExchange,
    		@Value("${eosc.amqp.exchange.transactions.routing-key:}")
    		String              transactionRoutingKey) {
        super(trainingResourceService);
		this.rabbitTemplate = rabbitTemplate;
	    this.objectMapper = objectMapper;
	    this.transactionExchange = transactionExchange;
	    this.transactionRoutingKey = transactionRoutingKey;
        this.trainingResourceService = trainingResourceService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<TrainingResourceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        trainingResourceService.addBulk(bundles, auth);
    }
    
    @PostMapping(path = "/publish/", produces = MediaType.APPLICATION_JSON_VALUE)
    public void publish(@RequestParam("id") @Parameter(allowReserved = true) String id, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException, IOException, TimeoutException {
    	List<ProcessInstanceRequestVariableDto> variables = new ArrayList<ProcessInstanceRequestVariableDto>();
    	variables.add(new ProcessInstanceRequestVariableDto("pid", id, EnumVariableType.STRING));
    	ProcessInstanceRequestDto instanceRequest = new ProcessInstanceRequestDto(null, null, "training-resource-publish", variables);
        this.sendMessage(instanceRequest);
    }
    
    private void sendMessage(ProcessInstanceRequestDto request) throws JsonProcessingException {
        final var payload = objectMapper.writeValueAsBytes(request);
        final var message = new Message(payload);
        this.rabbitTemplate.send(this.transactionExchange,this.transactionRoutingKey ,message);
    }
}
