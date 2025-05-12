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
public class EnrollementSteps {

    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String aaiEnrollement;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String cataloguesEnrollement;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String helpdeskEnrollement;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String monitoringEnrollement;

    public EnrollementSteps() {
    }

    public EnrollementSteps(String aaiEnrollement, String cataloguesEnrollement, String helpdeskEnrollement, String monitoringEnrollement ) {
    	this.aaiEnrollement = aaiEnrollement;
        this.cataloguesEnrollement = cataloguesEnrollement;
        this.helpdeskEnrollement = helpdeskEnrollement;
        this.monitoringEnrollement = monitoringEnrollement;
    }

    @Override
    public String toString() {
        return "Enrollement{" +
                " aaiEnrollement='" + aaiEnrollement + '\'' +
                ", cataloguesEnrollement='" + cataloguesEnrollement + '\'' +
                ", helpdeskEnrollement='" + helpdeskEnrollement + '\'' +
                ", monitoringEnrollement='" + monitoringEnrollement + '\'' +
                '}';
    }
    
    public String getAaiEnrollement() {
    	return aaiEnrollement;
    }

    public void setAaiEnrollement(String aaiEnrollement) {
        this.aaiEnrollement = aaiEnrollement;
    }


    public String getCataloguesEnrollement() {
    	return cataloguesEnrollement;
    }

    public void setCataloguesEnrollement(String cataloguesEnrollement) {
        this.cataloguesEnrollement = cataloguesEnrollement;
    }
    
    public String getHelpdeskEnrollement() {
    	return helpdeskEnrollement;
    }

    public void setHelpdeskEnrollement(String helpdeskEnrollement) {
        this.helpdeskEnrollement = helpdeskEnrollement;
    }
    
    public String getMonitoringEnrollement() {
    	return monitoringEnrollement;
    }

    public void setMonitoringEnrollement(String monitoringEnrollement) {
        this.monitoringEnrollement = monitoringEnrollement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrollementSteps that = (EnrollementSteps) o;
        return Objects.equals(aaiEnrollement, that.aaiEnrollement) 
        		&& Objects.equals(cataloguesEnrollement, that.cataloguesEnrollement)
        		&& Objects.equals(helpdeskEnrollement, that.helpdeskEnrollement)
        		&& Objects.equals(monitoringEnrollement, that.monitoringEnrollement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aaiEnrollement, cataloguesEnrollement, helpdeskEnrollement, monitoringEnrollement);
    }
}
