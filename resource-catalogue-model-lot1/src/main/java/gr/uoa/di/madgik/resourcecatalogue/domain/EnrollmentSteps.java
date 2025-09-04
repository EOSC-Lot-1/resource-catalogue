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
public class EnrollmentSteps {

    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ENROLLMENT_STATUS)
    private String aaiEnrollment;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ENROLLMENT_STATUS)
    private String cataloguesEnrollment;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ENROLLMENT_STATUS)
    private String helpdeskEnrollment;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ENROLLMENT_STATUS)
    private String monitoringEnrollment;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ENROLLMENT_STATUS)
    private String enrollmentGreenLight;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.ENROLLMENT_STATUS)
    private String legalFramework;


	public EnrollmentSteps() {
    }

    public EnrollmentSteps(String aaiEnrollment, String cataloguesEnrollment, String helpdeskEnrollment, String monitoringEnrollment,
    		String enrollmentGreenLight, String legalFramework) {
    	this.aaiEnrollment = aaiEnrollment;
        this.cataloguesEnrollment = cataloguesEnrollment;
        this.helpdeskEnrollment = helpdeskEnrollment;
        this.monitoringEnrollment = monitoringEnrollment;
        this.enrollmentGreenLight = enrollmentGreenLight;
        this.legalFramework = legalFramework;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                " aaiEnrollment='" + aaiEnrollment + '\'' +
                ", cataloguesEnrollment='" + cataloguesEnrollment + '\'' +
                ", helpdeskEnrollment='" + helpdeskEnrollment + '\'' +
                ", monitoringEnrollment='" + monitoringEnrollment + '\'' +
                ", enrollmentGreenLight='" + enrollmentGreenLight + '\'' +
                ", legalFramework='" + legalFramework + '\'' +
                '}';
    }
    
    public String getAaiEnrollment() {
    	return aaiEnrollment;
    }

    public void setAaiEnrollment(String aaiEnrollment) {
        this.aaiEnrollment = aaiEnrollment;
    }


    public String getCataloguesEnrollment() {
    	return cataloguesEnrollment;
    }

    public void setCataloguesEnrollment(String cataloguesEnrollment) {
        this.cataloguesEnrollment = cataloguesEnrollment;
    }
    
    public String getHelpdeskEnrollment() {
    	return helpdeskEnrollment;
    }

    public void setHelpdeskEnrollment(String helpdeskEnrollment) {
        this.helpdeskEnrollment = helpdeskEnrollment;
    }
    
    public String getMonitoringEnrollment() {
    	return monitoringEnrollment;
    }

    public void setMonitoringEnrollment(String monitoringEnrollment) {
        this.monitoringEnrollment = monitoringEnrollment;
    }

    public String getEnrollmentGreenLight() {
		return enrollmentGreenLight;
	}

	public void setEnrollmentGreenLight(String enrollmentGreenLight) {
		this.enrollmentGreenLight = enrollmentGreenLight;
	}

	public String getLegalFramework() {
		return legalFramework;
	}

	public void setLegalFramework(String legalFramework) {
		this.legalFramework = legalFramework;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrollmentSteps that = (EnrollmentSteps) o;
        return Objects.equals(aaiEnrollment, that.aaiEnrollment) 
        		&& Objects.equals(cataloguesEnrollment, that.cataloguesEnrollment)
        		&& Objects.equals(helpdeskEnrollment, that.helpdeskEnrollment)
        		&& Objects.equals(monitoringEnrollment, that.monitoringEnrollment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aaiEnrollment, cataloguesEnrollment, helpdeskEnrollment, monitoringEnrollment);
    }
}
