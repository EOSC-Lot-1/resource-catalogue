package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class OnboardingIntegration {

    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String serviceOfferFinalization;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String accountIntegration;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String aaiIntegration;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.INTEGRATION_STATUS)
    private String omsIntegration;

    public OnboardingIntegration() {
    }

    public OnboardingIntegration(String serviceOfferFinalization, String accountIntegration, String aaiIntegration, String omsIntegration ) {
    	this.serviceOfferFinalization = serviceOfferFinalization;
        this.accountIntegration = accountIntegration;
        this.aaiIntegration = aaiIntegration;
        this.omsIntegration = omsIntegration;
    }

    @Override
    public String toString() {
        return "OnboardingIntegration{" +
                " serviceOfferFinalization='" + serviceOfferFinalization + '\'' +
                ", accountIntegration='" + accountIntegration + '\'' +
                ", aaiIntegration='" + aaiIntegration + '\'' +
                ", omsIntegration='" + omsIntegration + '\'' +
                '}';
    }
    
    public String getServiceOfferFinalization() {
    	return serviceOfferFinalization;
    }

    public void setServiceOfferFinalization(String serviceOfferFinalization) {
        this.serviceOfferFinalization = serviceOfferFinalization;
    }


    public String getAccountIntegration() {
    	return accountIntegration;
    }

    public void setAccountIntegration(String accountIntegration) {
        this.accountIntegration = accountIntegration;
    }
    
    public String getaaiIntegration() {
    	return aaiIntegration;
    }

    public void setAaiIntegration(String aaiIntegration) {
        this.aaiIntegration = aaiIntegration;
    }
    
    public String getOmsIntegration() {
    	return omsIntegration;
    }

    public void setOmsIntegration(String omsIntegration) {
        this.omsIntegration = omsIntegration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnboardingIntegration that = (OnboardingIntegration) o;
        return Objects.equals(serviceOfferFinalization, that.serviceOfferFinalization) 
        		&& Objects.equals(accountIntegration, that.accountIntegration)
        		&& Objects.equals(aaiIntegration, that.aaiIntegration)
        		&& Objects.equals(omsIntegration, that.omsIntegration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceOfferFinalization, accountIntegration, aaiIntegration, omsIntegration);
    }
}
