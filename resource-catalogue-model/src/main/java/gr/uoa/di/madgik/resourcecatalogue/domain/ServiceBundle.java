package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceBundle extends Bundle<Service> {

    @XmlElement
    private String status;

    @XmlElement
    @FieldValidation(nullable = true)
    private ResourceExtras resourceExtras;
    
    @XmlElementWrapper(name = "sites")
    @XmlElement(name = "sites")
    private List<Site> sites;
    
    @XmlElement
    private String auditState;

    public ServiceBundle() {
        // No arg constructor
    }

    public ServiceBundle(Service service) {
        this.setService(service);
        this.setMetadata(null);
    }

    public ServiceBundle(Service service, Metadata metadata) {
        this.setService(service);
        this.setMetadata(metadata);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @XmlElement(name = "service")
    public Service getService() {
        return this.getPayload();
    }

    public void setService(Service service) {
        this.setPayload(service);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ResourceExtras getResourceExtras() {
        return resourceExtras;
    }

    public void setResourceExtras(ResourceExtras resourceExtras) {
        this.resourceExtras = resourceExtras;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }
    
    public List<Site> getSites() {
        return sites;
    }

    public void setSites( List<Site>  sites) {
        this.sites = sites;
    }

    @Override
    public String toString() {
        return "ServiceBundle{" +
                "status='" + status + '\'' +
                ", resourceExtras=" + resourceExtras +
                '}';
    }
}
