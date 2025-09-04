package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceBundle extends Bundle<Service> {

	@XmlElement
    @Schema(description = "Private")
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;

    @XmlElement
    @Schema(description = "Public")
    @FieldValidation(nullable = true)
    private ResourceExtras resourceExtras;

    @XmlElementWrapper(name = "sites")
    @XmlElement(name = "sites")
    @Schema(description = "Public")
    private List<Site> sites;
    
    @XmlElement()
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private Boolean resubmit;
    
	@XmlElement
    @Schema(description = "Private")
    private String auditState;
    
    @XmlElement
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private OnboardingIntegration onboardingIntegration;
    
    @XmlElement
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
	private String resourceOrganisationGroupID;
    
    @XmlElement
    @Schema(description = "Public")
    @FieldValidation(nullable = true)
	private String nodeId;

    @XmlElement()
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private Boolean offboardRequestPending;

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
    
    public Boolean getResubmit() {
        return resubmit;
    }

    public void setResubmit(Boolean resubmit) {
        this.resubmit = resubmit;
    }
	
    public Boolean getOffboardRequestPending() {
		return offboardRequestPending;
	}

	public void setOffboardRequestPending(Boolean offboardRequestPending) {
		this.offboardRequestPending = offboardRequestPending;
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

    public OnboardingIntegration getOnboardingIntegration() {
        return onboardingIntegration;
    }

    public void setOnboardingIntegration( OnboardingIntegration  onboardingIntegration) {
        this.onboardingIntegration = onboardingIntegration;
    }

    public String getResourceOrganisationGroupID() {
    	return resourceOrganisationGroupID;
    }

    public void setResourceOrganisationGroupID(String resourceOrganisationGroupID) {
        this.resourceOrganisationGroupID = resourceOrganisationGroupID;
    }
    
    public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

    @Override
    public String toString() {
        return "ServiceBundle{" +
                "status='" + status + '\'' +
                ", resourceExtras=" + resourceExtras +
                ", onboardingIntegration=" + onboardingIntegration +
                '}';
    }
}