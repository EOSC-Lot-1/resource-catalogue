package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Acknowledgement {

    @XmlElement()
    @Schema
    private Boolean catalogueStoreAck;

	@XmlElement()
    @Schema
    private Boolean resourceHubAck;

	@XmlElement()
    @Schema
    private Boolean monitoringServiceAck;

    @XmlElement()
    @Schema
    private Boolean securityContactAck;
    
    public Acknowledgement() {
    }

    public Acknowledgement(Boolean catalogueStoreAck, Boolean resourceHubAck, Boolean monitoringServiceAck, Boolean securityContactAck ) {
    	this.catalogueStoreAck = catalogueStoreAck;
        this.resourceHubAck = resourceHubAck;
        this.monitoringServiceAck = monitoringServiceAck;
        this.securityContactAck = securityContactAck;
    }

	public Boolean getCatalogueStoreAck() {
		return catalogueStoreAck;
	}

	public void setCatalogueStoreAck(Boolean catalogueStoreAck) {
		this.catalogueStoreAck = catalogueStoreAck;
	}

	public Boolean getResourceHubAck() {
		return resourceHubAck;
	}

	public void setResourceHubAck(Boolean resourceHubAck) {
		this.resourceHubAck = resourceHubAck;
	}

	public Boolean getMonitoringServiceAck() {
		return monitoringServiceAck;
	}

	public void setMonitoringServiceAck(Boolean monitoringServiceAck) {
		this.monitoringServiceAck = monitoringServiceAck;
	}

	public Boolean getSecurityContactAck() {
		return securityContactAck;
	}

	public void setSecurityContactAck(Boolean securityContactAck) {
		this.securityContactAck = securityContactAck;
	}

	@Override
	public int hashCode() {
		return Objects.hash(catalogueStoreAck, monitoringServiceAck, resourceHubAck, securityContactAck);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Acknowledgement other = (Acknowledgement) obj;
		return Objects.equals(catalogueStoreAck, other.catalogueStoreAck)
				&& Objects.equals(monitoringServiceAck, other.monitoringServiceAck)
				&& Objects.equals(resourceHubAck, other.resourceHubAck)
				&& Objects.equals(securityContactAck, other.securityContactAck);
	}
    

    
}
