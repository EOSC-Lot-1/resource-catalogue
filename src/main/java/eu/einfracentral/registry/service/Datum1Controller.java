package eu.einfracentral.registry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by pgl on 27/6/2017.
 */


@RestController
@RequestMapping("datum1")
public class Datum1Controller {

    @Autowired
    Datum1Interface datum1Service;

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getComponent(@PathVariable("id") String id) throws Throwable {
        return new ResponseEntity<>(datum1Service.get(id), HttpStatus.OK);
    }

}
