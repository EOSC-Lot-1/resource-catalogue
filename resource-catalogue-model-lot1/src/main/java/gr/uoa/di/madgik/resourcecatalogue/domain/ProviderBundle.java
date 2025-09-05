package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ProviderBundle extends Bundle<Provider> {

    @XmlElement
    @Schema(description = "Private")
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;

    @XmlElement
    @Schema(description = "Private")
//    @VocabularyValidation(type = Vocabulary.Type.TEMPLATE_STATE)
    private String templateStatus;
    
    @XmlElement()
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private Boolean resubmit;
    
    @XmlElement
    @Schema(description = "Private")
    private String auditState;
    
    @XmlElement()
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private String resourceOrganisationGroupID;
    
    @XmlElement
    @Schema(description = "Public")
    @FieldValidation(nullable = true)
    private NodeInfo nodeInfo;

    @XmlElementWrapper(name = "transferContactInformation")
    @XmlElement(name = "transferContactInformation")
    @Schema(description = "Private")
    private List<ContactInfoTransfer> transferContactInformation;

    public ProviderBundle() {
        // no arg constructor
    }

    public ProviderBundle(Provider provider) {
        this.setProvider(provider);
        this.setMetadata(null);
    }

    public ProviderBundle(Provider provider, Metadata metadata) {
        this.setProvider(provider);
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

    @XmlElement(name = "provider")
    public Provider getProvider() {
        return this.getPayload();
    }

    public void setProvider(Provider provider) {
        this.setPayload(provider);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplateStatus() {
        return templateStatus;
    }

    public void setTemplateStatus(String templateStatus) {
        this.templateStatus = templateStatus;
    }
    
    public Boolean getResubmit() {
        return resubmit;
    }

    public void setResubmit(Boolean resubmit) {
        this.resubmit = resubmit;
    }

    public String getAuditState() {
        return auditState;
    }

    public void setAuditState(String auditState) {
        this.auditState = auditState;
    }

    public String getResourceOrganisationGroupID() {
    	return resourceOrganisationGroupID;
    }

    public void setResourceOrganisationGroupID(String resourceOrganisationGroupID) {
        this.resourceOrganisationGroupID = resourceOrganisationGroupID;
    }

    public NodeInfo getNodeInfo() {
		return nodeInfo;
	}

	public void setNodeInfo(NodeInfo nodeInfo) {
		this.nodeInfo = nodeInfo;
	}
    
    public List<ContactInfoTransfer> getTransferContactInformation() {
        return transferContactInformation;
    }

    public void setTransferContactInformation(List<ContactInfoTransfer> transferContactInformation) {
        this.transferContactInformation = transferContactInformation;
    }

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProviderBundle other = (ProviderBundle) obj;
		return Objects.equals(auditState, other.auditState) && Objects.equals(nodeInfo, other.nodeInfo)
				&& Objects.equals(resourceOrganisationGroupID, other.resourceOrganisationGroupID)
				&& Objects.equals(resubmit, other.resubmit) && Objects.equals(templateStatus, other.templateStatus)
				&& Objects.equals(transferContactInformation, other.transferContactInformation);
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(auditState, nodeInfo, resourceOrganisationGroupID, resubmit,
				templateStatus, transferContactInformation);
		return result;
	}
    
    @Override
    public String toString() {
        return "ProviderBundle{" +
                "status='" + status + '\'' +
                ", nodeInfo=" + nodeInfo + 
                "} " + super.toString();
    }
}
