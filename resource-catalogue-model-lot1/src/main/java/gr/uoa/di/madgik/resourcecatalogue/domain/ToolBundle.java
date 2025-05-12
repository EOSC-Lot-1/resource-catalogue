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
    
    @Override
    public String toString() {
        return "ToolBundle{" +
                "status='" + status + '\'' +
                "security='" + security + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ToolBundle that = (ToolBundle) o;
        return Objects.equals(status, that.status) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, security);
    }
}



