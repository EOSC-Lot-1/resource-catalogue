package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.Resource;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@org.springframework.stereotype.Service("serviceService")
public class ServiceManager extends ResourceManager<Service> implements ServiceService {
    @Autowired
    private AddendaManager addendaManager;

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        //TODO: id is null when service is added via frontend, so make sure to make one, based on provider
        if (!service.getId().contains(".")) {
            service.setId(java.util.UUID.randomUUID().toString());
        }
        if (exists(service)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        return super.add(validate(service));
    }

    private Service fixVersion(Service service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return service;
    }

    @Override
    public Service update(Service service) {
        service = validate(service);
        Service existingService = get(service.getId());
        Addenda addenda = ensureAddenda(service.getId());
        fixVersion(existingService); //remove this when it has ran for all services
        if (service.getVersion().equals(existingService.getVersion())) {
            addenda.setModifiedAt(System.currentTimeMillis());
            addenda.setModifiedBy("pgl");
            super.update(service);
        } else {
            addenda.setRegisteredAt(System.currentTimeMillis());
            addenda.setRegisteredBy("pgl");
            existingService.setId(UUID.randomUUID().toString());
            super.update(existingService);
            super.add(service);
        }
        try {
            addendaManager.update(addenda);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return service;
    }

    private Addenda ensureAddenda(String id) {
        Addenda ret = null;
        Resource existingAddendaResource = addendaManager.where("service", id, false);
        if (existingAddendaResource != null) {
            try {
                ret = parserPool.deserialize(existingAddendaResource, Addenda.class).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                ret = makeAddenda(id);
            }
        } else {
            ret = makeAddenda(id);
        }
        return ret;
    }

    private Addenda makeAddenda(String id) {
        Addenda ret = new Addenda();
        ret.setId(UUID.randomUUID().toString());
        ret.setService(id);
        try {
            addendaManager.add(ret);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public Service validate(Service service) {
        return fixVersion(service);
    }
}
