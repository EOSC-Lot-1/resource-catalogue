package eu.einfracentral.validators;

import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ServiceBundleService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

import static eu.einfracentral.utils.VocabularyValidationUtils.validateCategories;
import static eu.einfracentral.utils.VocabularyValidationUtils.validateScientificDomains;

@Component("serviceValidator")
public class ServiceDatasourceValidator implements Validator {

    private static final Logger logger = LogManager.getLogger(ServiceDatasourceValidator.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final String catalogueName;

    @Autowired
    public ServiceDatasourceValidator(@Value("${project.catalogue.name}") String catalogueName,
                                      ServiceBundleService<ServiceBundle> serviceBundleService) {
        this.catalogueName = catalogueName;
        this.serviceBundleService = serviceBundleService;
    }


    @Override
    public boolean supports(Class<?> clazz) {
        return ServiceBundle.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Service service = ((ServiceBundle) target).getPayload();
        //TODO: Probably not needed -> related/required field validation was added on FieldValidator
//        if (service.getRelatedResources() != null && !service.getRelatedResources().isEmpty()){
//            validateRelatedResources(service, errors);
//        }
//        if (service.getRequiredResources() != null && !service.getRequiredResources().isEmpty()){
//            validateRequiredResources(service, errors);
//        }
        validateCategories(service.getCategories());
        validateScientificDomains(service.getScientificDomains());
    }

    private void validateRequiredResources(Service service, Errors errors) {
        validateServiceResources(service.getRelatedResources(), service.getCatalogueId(), "relatedResources", errors);
    }

    private void validateRelatedResources(Service service, Errors errors) {
        validateServiceResources(service.getRequiredResources(), service.getCatalogueId(), "requiredResources", errors);
    }


    private void validateServiceResources(List<String> resources, String catalogue, String field, Errors errors) {
        for (String resource : resources) {
            try {
                serviceBundleService.get(resource, catalogue);
            } catch (Exception e) {
                try {
                    serviceBundleService.get(resource, catalogueName);
                } catch (Exception ex) {
//                    errors.rejectValue("relatedResources", field + ".invalid", String.format("Could not find Service with id [%s] in Catalogue [%s]", resource, catalogue));
                    throw new ValidationException(String.format("Could not find Service with id [%s] in Catalogue [%s]", resource, catalogue));
                }
            }
        }
    }
}
