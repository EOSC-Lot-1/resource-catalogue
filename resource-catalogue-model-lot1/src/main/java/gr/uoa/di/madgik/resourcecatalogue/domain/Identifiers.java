package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Identifiers {

    @XmlElement()
    private String originalId;

    public Identifiers() {
    }

    public Identifiers(Identifiers identifiers) {
        this.originalId = identifiers.getOriginalId();
    }

    @Override
    public String toString() {
        return "Identifiers{" +
                "originalId='" + originalId + '\'' +
                '}';
    }

    public static void createOriginalId(Bundle<?> bundle) {
        if (bundle.getIdentifiers() != null) {
            bundle.getIdentifiers().setOriginalId(bundle.getId());
        } else {
            Identifiers identifiers = new Identifiers();
            identifiers.setOriginalId(bundle.getId());
            bundle.setIdentifiers(identifiers);
        }
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

	@Override
	public int hashCode() {
		return Objects.hash(originalId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identifiers other = (Identifiers) obj;
		return Objects.equals(originalId, other.originalId);
	}
    
}
