package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.EmailValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.GeoLocationVocValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Tool implements Identifiable {

    // Basic Information
    /**
     * A persistent identifier, a unique reference to the Resource.
     */
    @XmlElement
    @Schema(example = "(required on PUT only)")
    private String id;

    /**
     * The human-readable name of the learning resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;
    
    /**
     * The name of the organisation that manages or delivers the resource, or that coordinates the Resource delivery in a federated scenario.
     */
    @XmlElement()
    @Schema
    private String resourceOrganisation;
    
    /**
     * The pid of the provider that manages the resource.
     */
    @XmlElement()
    @Schema
    private String resourceProvider;
    
    /**
     * List of other Resources that are commonly used with this Resource.
     */
    @XmlElementWrapper(name = "relatedResources")
    @XmlElement(name = "relatedResource")
    @Schema
    @FieldValidation(nullable = true, containsId = true, containsResourceId = true)
    private List<String> relatedResources;

    // Detailed & Access Information
    /**
     * A brief synopsis about or description of the tool.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String description;

    /**
     * The keyword(s) or tag(s) used to describe the resource.
     */
    @XmlElementWrapper(name = "keywords", required = true)
    @XmlElement(name = "keyword")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private List<String> keywords;

    /**
     * A license document that applies to this content, typically indicated by URL.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TOOL_LICENSE)
    private String license;

    /**
     * The version date for the most recently published or broadcast resource.
     */
    @XmlElement(required = true)
    @Schema(example = "2020-01-01", required = true)
    @FieldValidation
    private Date versionDate;


    // Learning Information
    /**
     * The target infrastructure of the TOSCA template
     */
    @XmlElementWrapper(name = "targetInfrastructure", required = true)
    @XmlElement(name = "targetInfrastructure")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TOOL_TARGET_INFRASTRUCTURE)
    private List<String> targetInfrastructure;


    // Learning Information
    /**
     * The principal users(s) for which the learning resource was designed.
     */
    @XmlElementWrapper(name = "targetGroups")
    @XmlElement(name = "targetGroup")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TARGET_USER)
    private List<String> targetGroups;
    
    /**
     * The first and last name of the user that is uploading the TOSCA template.
     */
    @XmlElement(name = "author")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;
    
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private Boolean deprecated;

    // Classification Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @XmlElementWrapper(name = "scientificDomain", required = true)
    @XmlElement(name = "scientificDomain")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation()
    private List<ServiceProviderDomain> scientificDomains;

    // Management Information
    /**
     * The URL to a webpage to ask more information from the Provider about this Resource.
     */
    @XmlElement
    @Schema(example = "https://example.com")
    @FieldValidation(nullable = true)
    private URL helpdeskPage;
    
    
    /**
     * The minimum credits estimation required to deploy this tool on the EOSC EU Node
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String creditCost;
    
//    /**
//     * The URL to the location of the actual tool.
//     */
//    @XmlElement
//    @Schema
//    @FieldValidation(nullable = true)
//    private String path;
    
 
    /**
     * Email of the Resource's main contact person/manager.
     */
    @XmlElement()
    @Schema()
    @EmailValidation(nullable = true)
    private String email;

    public Tool() {
    }

    public Tool(String id, String name, String resourceOrganisation, String resourceProvider, List<String> relatedResources, List<String> resourceProviders, String author, String description, List<String> keywords, String license, Date versionDate, List<String> targetGroups, List<String> learningResourceTypes, List<String> learningOutcomes, String expertiseLevel, List<String> contentResourceTypes, List<String> qualifications, String duration, Boolean deprecated, List<ServiceProviderDomain> scientificDomains, URL helpdeskPage, String path, String creditCost, String email) {
        this.id = id;
        this.name = name;
        this.resourceOrganisation = resourceOrganisation;
        this.resourceProvider = resourceProvider;
        this.relatedResources = relatedResources;
        this.author = author;
        this.description = description;
        this.keywords = keywords;
        this.license = license;
        this.versionDate = versionDate;
        this.deprecated = deprecated;
        this.scientificDomains = scientificDomains;
        this.helpdeskPage = helpdeskPage;
        this.creditCost = creditCost;
        //this.path = path;
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tool that = (Tool) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(resourceOrganisation, that.resourceOrganisation) && Objects.equals(resourceProvider, that.resourceProvider) && Objects.equals(description, that.description) && Objects.equals(keywords, that.keywords) && Objects.equals(license, that.license) && Objects.equals(versionDate, that.versionDate) && Objects.equals(scientificDomains, that.scientificDomains) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, resourceOrganisation, resourceProvider, description, keywords, license, versionDate, targetInfrastructure , scientificDomains, email);
    }

    @Override
    public String toString() {
        return "TrainingResource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", resourceOrganisation='" + resourceOrganisation + '\'' +
                ", resourceProvider='" + resourceProvider + '\'' +
                ", author=" + author +
                ", description='" + description + '\'' +
                ", keywords=" + keywords +
                ", license='" + license + '\'' +
                ", versionDate=" + versionDate +
                ", targetInfrastructure=" + targetInfrastructure +
                ", deprecated=" + deprecated +
                ", scientificDomains=" + scientificDomains +
                ", email=" + email +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceOrganisation() {
        return resourceOrganisation;
    }

    public void setResourceOrganisation(String resourceOrganisation) {
        this.resourceOrganisation = resourceOrganisation;
    }
    
    public String getResourceProvider() {
        return resourceProvider;
    }

    public void setResourceProvider(String resourceProvider) {
        this.resourceProvider = resourceProvider;
    }
    
    public List<String> getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(List<String> relatedResources) {
        this.relatedResources = relatedResources;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Date getVersionDate() {
        return versionDate;
    }

    public void setVersionDate(Date versionDate) {
        this.versionDate = versionDate;
    }


    public List<String> getTargetInfrastructure() {
        return targetInfrastructure;
    }

    public void setTargetInfrastructure(List<String> targetInfrastructure) {
        this.targetInfrastructure = targetInfrastructure;
    }
    

    public List<String> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(List<String> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<ServiceProviderDomain> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }
    
    public URL getHelpdeskPage() {
        return helpdeskPage;
    }

    public void setHelpdeskPage(URL helpdeskPage) {
        this.helpdeskPage = helpdeskPage;
    }

    public String getCreditCost() {
        return creditCost;
    }

    public void setCreditCost(String creditCost) {
        this.creditCost = creditCost;
    }
    
//    public String getPath() {
//        return path;
//    }
//
//    public void setPath(String path) {
//        this.path = path;
//    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}