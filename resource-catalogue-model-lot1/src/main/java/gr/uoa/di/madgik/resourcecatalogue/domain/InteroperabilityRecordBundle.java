package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InteroperabilityRecordBundle extends Bundle<InteroperabilityRecord> {

	@XmlElement
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;
    
    @XmlElement()
    @FieldValidation(nullable = true)
    private Boolean resubmit;
    
    @XmlElement
    private String auditState;

    public InteroperabilityRecordBundle() {
    }

    public InteroperabilityRecordBundle(InteroperabilityRecord interoperabilityRecord) {
        this.setInteroperabilityRecord(interoperabilityRecord);
        this.setMetadata(null);
    }

    public InteroperabilityRecordBundle(InteroperabilityRecord interoperabilityRecord, Metadata metadata) {
        this.setInteroperabilityRecord(interoperabilityRecord);
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

    @XmlElement(name = "interoperabilityRecord")
    public InteroperabilityRecord getInteroperabilityRecord() {
        return this.getPayload();
    }

    public void setInteroperabilityRecord(InteroperabilityRecord interoperabilityRecord) {
        this.setPayload(interoperabilityRecord);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InteroperabilityRecordBundle that = (InteroperabilityRecordBundle) o;
        return Objects.equals(status, that.status) && Objects.equals(auditState, that.auditState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), status, auditState);
    }
}
