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
	@Schema(description = "Public")
	@FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.DS_PROTOCOL)
	private String protocol;

	@XmlElement()
	@Schema(description = "Public", requiredMode = Schema.RequiredMode.REQUIRED)
	@FieldValidation()
	private URL baseUrl;

	@XmlElement()
	@Schema(description = "Public")
	@FieldValidation(nullable = true)
	private List<String> sets;
	
	@XmlElement()
	@Schema(description = "Public")
	@FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.DS_OAI_FORMATS)
	private String format;
	
	@XmlElement()
	@Schema(description = "Public")
	@FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.DS_OAI_COMPATIBILITY)
	private String compatibility;
	
	@XmlElement
	@Schema(description = "Public")
	@FieldValidation(nullable = true)
	private Boolean openAIRECompliance;
	
    @XmlElement()
    @Schema(description = "Public")
    @FieldValidation(nullable = true)
    private AlternativeIdentifier repositoryIdentifier;
    
    @XmlElementWrapper(name = "alternativeIdentifiers")
    @XmlElement(name = "alternativeIdentifier")
    @Schema(description = "Public")
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OAIPMHInfo other = (OAIPMHInfo) obj;
		return Objects.equals(alternativeIdentifiers, other.alternativeIdentifiers)
				&& Objects.equals(baseUrl, other.baseUrl) && Objects.equals(compatibility, other.compatibility)
				&& Objects.equals(format, other.format) && Objects.equals(openAIRECompliance, other.openAIRECompliance)
				&& Objects.equals(protocol, other.protocol)
				&& Objects.equals(repositoryIdentifier, other.repositoryIdentifier) && Objects.equals(sets, other.sets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(alternativeIdentifiers, baseUrl, compatibility, format, openAIRECompliance, protocol,
				repositoryIdentifier, sets);
	}
}
