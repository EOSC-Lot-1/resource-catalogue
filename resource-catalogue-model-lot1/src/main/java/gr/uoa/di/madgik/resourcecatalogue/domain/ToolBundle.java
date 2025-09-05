package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ToolBundle extends Bundle<Tool> {
	
	@XmlElement
    @Schema(description = "Private")
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;
    
    @XmlElement
    @Schema(description = "Private")
    private ToolSecurity security;
    
    @XmlElement
    @Schema(description = "Private")
    private boolean contributorProvided;

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

    public ToolBundle() {
        // No arg constructor
    }

    public ToolBundle(Tool tool) {
        this.setTool(tool);
        this.setMetadata(null);
    }

    public ToolBundle(Tool tool, Metadata metadata) {
        this.setTool(tool);
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

    @XmlElement(name = "tool")
    public Tool getTool() {
        return this.getPayload();
    }

    public void setTool(Tool tool) {
        this.setPayload(tool);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public ToolSecurity getSecurity() {
        return security;
    }

    public void setSecurity(ToolSecurity security) {
        this.security = security;
    }

    public Boolean getContributorProvided() {
        return contributorProvided;
    }

    public void setContributorProvided(Boolean contributorProvided) {
        this.contributorProvided = contributorProvided;
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
	
    public Boolean getOffboardRequestPending() {
		return offboardRequestPending;
	}

	public void setOffboardRequestPending(Boolean offboardRequestPending) {
		this.offboardRequestPending = offboardRequestPending;
	}

	@Override
    public String toString() {
        return "ToolBundle{" +
                "status='" + status + '\'' +
                "security='" + security + '\'' +
                '}';
    }

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToolBundle other = (ToolBundle) obj;
		return contributorProvided == other.contributorProvided && Objects.equals(nodeId, other.nodeId)
				&& Objects.equals(offboardRequestPending, other.offboardRequestPending)
				&& Objects.equals(resourceOrganisationGroupID, other.resourceOrganisationGroupID)
				&& Objects.equals(security, other.security);
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(contributorProvided, nodeId, offboardRequestPending,
				resourceOrganisationGroupID, security);
		return result;
	}
}



