package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.net.URL;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class OAIPMHInfo {

	@XmlElement()
	@Schema
	@FieldValidation(nullable = true)
	private String protocol;

	@XmlElement()
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	@FieldValidation()
	private URL baseUrl;

	@XmlElement()
	@Schema
	@FieldValidation(nullable = true)
	private List<String> sets;
	
	@XmlElement()
	@Schema
	@FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.DS_OAI_FORMATS)
	private String format;
	
	@XmlElement()
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	@FieldValidation(containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.DS_OAI_COMPATIBILITY)
	private String compatibility;
	
	@XmlElement
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	@FieldValidation()
	private Boolean openAIRECompliance;
	
    @XmlElement()
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private AlternativeIdentifier repositoryIdentifier;
    
    @XmlElementWrapper(name = "alternativeIdentifiers")
    @XmlElement(name = "alternativeIdentifier")
    @Schema
    @FieldValidation(nullable = true)
    private List<AlternativeIdentifier> alternativeIdentifiers;

	public OAIPMHInfo() {
	}

	public OAIPMHInfo(String protocol, URL baseUrl, List<String> sets, String format, Boolean openAIRECompliance) {
		this.protocol = protocol;
		this.baseUrl = baseUrl;
		this.sets = sets;
		this.format = format;
		this.openAIRECompliance = openAIRECompliance;
	}

	@Override
	public String toString() {
		return "OAIPMHInfo{" + " protocol='" + protocol + '\'' + ", sets='" + sets + '\''
				+ ", baseUrl='" + baseUrl + '\'' + ", format='" + format + '\'' + '}';
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public List<String> getSets() {
		return sets;
	}

	public void setSets(List<String> sets) {
		this.sets = sets;
	}
	
	public URL getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(URL baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
	
	public String getCompatibility() {
		return compatibility;
	}

	public void setCompatibility(String compatibility) {
		this.compatibility = compatibility;
	}
	
	public Boolean getOpenAIRECompliance() {
		return openAIRECompliance;
	}

	public void setOpenAIRECompliance(Boolean openAIRECompliance) {
		this.openAIRECompliance = openAIRECompliance;
	}

	public AlternativeIdentifier getRepositoryIdentifier() {
		return repositoryIdentifier;
	}

	public void setFormat(AlternativeIdentifier repositoryIdentifier) {
		this.repositoryIdentifier = repositoryIdentifier;
	}
	

	public List<AlternativeIdentifier> getAlternativeIdentifiers() {
		return alternativeIdentifiers;
	}

	public void setFormat(List<AlternativeIdentifier> alternativeIdentifiers) {
		this.alternativeIdentifiers = alternativeIdentifiers;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		OAIPMHInfo that = (OAIPMHInfo) o;
		return Objects.equals(protocol, that.protocol) && Objects.equals(sets, that.sets)
				&& Objects.equals(baseUrl, that.baseUrl) && Objects.equals(format, that.format)
				&& Objects.equals(openAIRECompliance, that.openAIRECompliance);
	}

	@Override
	public int hashCode() {
		return Objects.hash(protocol, sets, baseUrl, format, openAIRECompliance);
	}
}
