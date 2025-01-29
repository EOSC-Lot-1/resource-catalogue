package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ToolBundle extends Bundle<Tool> {
	
	@XmlElement
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;
    
    @XmlElement
    private ToolSecurity security;
    
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



