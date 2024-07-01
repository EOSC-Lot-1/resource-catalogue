package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ToolBundle extends Bundle<Tool> {

    @XmlElement
    private String status;

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

    @Override
    public String toString() {
        return "ToolBundle{" +
                "status='" + status + '\'' +
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
        return Objects.hash(super.hashCode(), status);
    }
}



