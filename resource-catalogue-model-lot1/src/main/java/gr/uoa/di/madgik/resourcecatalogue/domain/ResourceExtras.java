package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceExtras {

    @XmlElementWrapper(name = "eoscIFGuidelines")
    @XmlElement(name = "eoscIFGuideline")
    @Schema(description = "Public")
    @FieldValidation(nullable = true)
    private List<EOSCIFGuidelines> eoscIFGuidelines;

    public ResourceExtras() {
    }

    public ResourceExtras(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }

    @Override
    public String toString() {
        return "ResourceExtras{" +
                "eoscIFGuidelines=" + eoscIFGuidelines +
                '}';
    }

    public List<EOSCIFGuidelines> getEoscIFGuidelines() {
        return eoscIFGuidelines;
    }

    public void setEoscIFGuidelines(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }

	@Override
	public int hashCode() {
		return Objects.hash(eoscIFGuidelines);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceExtras other = (ResourceExtras) obj;
		return Objects.equals(eoscIFGuidelines, other.eoscIFGuidelines);
	}
    
}
