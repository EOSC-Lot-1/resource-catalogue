package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//the below line contains the only produces needed for spring to work in the entire project; all others are there until springfox fix their bugs
@RequestMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
public abstract class ResourceCrudController<T extends Identifiable> {
    protected final ResourceService<T> service;

    private static final Logger logger = LogManager.getLogger(ResourceCrudController.class);

    protected ResourceCrudController(ResourceService<T> service) {
        this.service = service;
    }

    @GetMapping(path = "{id}")
    public ResponseEntity<T> get(@PathVariable("id") String id, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    @PostMapping
            public ResponseEntity<T> add(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        ResponseEntity<T> ret = new ResponseEntity<>(service.save(t), HttpStatus.CREATED);
        logger.debug("User {} created a new {} with id {}", (auth == null) ? "unknown" : auth.getName(),
                t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @PutMapping(path = "{id}")
    public ResponseEntity<T> update(@RequestBody T t, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        ResponseEntity<T> ret = new ResponseEntity<>(service.save(t), HttpStatus.OK);
        logger.debug("User {} updated {} with id {}", (auth == null) ? "unknown" : auth.getName(),
                t.getClass().getSimpleName(), t.getId());
        return ret;
    }

    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> validate(@RequestBody T t, @Parameter(hidden = true) Authentication auth) {
        service.validate(t);
        logger.debug("User {} validated {} with id {}", (auth == null) ? "unknown" : auth.getName(),
                t.getClass().getSimpleName(), t.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> delete(@RequestBody T t, @Parameter(hidden = true) Authentication auth) throws ResourceNotFoundException {
        service.delete(t);
        logger.debug("User {} deleted {} with id {}", (auth == null) ? "unknown" : auth.getName(),
                t.getClass().getSimpleName(), t.getId());
        return new ResponseEntity<>(t, HttpStatus.OK);
    }

    @DeleteMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<T>> delAll(@Parameter(hidden = true) Authentication auth) {
        ResponseEntity<List<T>> ret = new ResponseEntity<>(service.delAll(), HttpStatus.OK);
        logger.debug("User {} deleted a list of resources {}", (auth == null) ? "unknown" : auth.getName(), ret);
        return ret;
    }

    // Filter a list of Resources based on a set of filters.
    @Browse
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<T>> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams, @Parameter(hidden = true) Authentication auth) {
        FacetFilter ff = FacetFilterUtils.createFacetFilter(allRequestParams);
        return new ResponseEntity<>(service.getAll(ff, null), HttpStatus.OK);
    }

    @GetMapping(path = "ids/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<T>> getSome(@PathVariable String[] ids, @Parameter(hidden = true) Authentication auth) {
        return new ResponseEntity<>(service.getSome(ids), HttpStatus.OK);
    }

    @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field, @Parameter(hidden = true) Authentication auth) throws NoSuchFieldException {
        return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
    }
}
