package gr.uoa.di.madgik.resourcecatalogue.domain;

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
    @FieldValidation
    private String id;

    /**
     * The human-readable name of the learning resource.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    /**
     * The name(s) of (all) the Provider(s) that manage or deliver the Resource in federated scenarios.
     */
    @XmlElementWrapper(name = "resourceProviders")
    @XmlElement(name = "resourceProvider")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Provider.class)
    private List<String> resourceProviders;
    
    /**
     * The name of the organisation that manages or delivers the resource, or that coordinates the Resource delivery in a federated scenario.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Provider.class)
    private String resourceOrganisation;

    /**
     * The name of entity(ies) authoring the resource.
     */
    @XmlElementWrapper(name = "authors", required = true)
    @XmlElement(name = "author")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<String> authors;
    
    
    /**
     * List of other Resources that are commonly used with this Resource.
     */
    @XmlElementWrapper(name = "relatedResources")
    @XmlElement(name = "relatedResource")
    @Schema
    @FieldValidation(nullable = true, containsId = true, containsResourceId = true)
    private List<String> relatedResources;

//    /**
//     * The URL that resolves to the learning resource or to a "landing page" for the resource that contains important
//     * contextual information including the direct resolvable link to the resource, if applicable.
//     */
//    @XmlElement(required = true)
//    @Schema(example = "https://example.com", required = true)
//    @FieldValidation
//    private URL url;
//
//    /**
//     * The designation of identifier scheme used for the resource URL. It represents the type of the URL of the resource,
//     * that is the used scheme (e.g., Web Address URL, DOI, ARK, etc.).
//     */
//    @XmlElement
//    @Schema
//    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
//    @VocabularyValidation(type = Vocabulary.Type.TR_URL_TYPE)
//    private String urlType;

    /**
     * The name(s) of (all) the Provider(s) that manage or deliver the Resource in federated scenarios.
     
    @XmlElementWrapper(name = "eoscRelatedServices")
    @XmlElement(name = "eoscRelatedService")
    @Schema
    @FieldValidation(nullable = true, containsId = true, containsResourceId = true)
    private List<String> eoscRelatedServices;
     */

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
    @XmlElementWrapper(name = "keywords")
    @XmlElement(name = "keyword")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> keywords;

    /**
     * A license document that applies to this content, typically indicated by URL.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String license;

//    /**
//     * The access status of a resource (open, restricted, paid).
//     */
//    @XmlElement(required = true)
//    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
//    @FieldValidation(containsId = true, idClass = Vocabulary.class)
//    @VocabularyValidation(type = Vocabulary.Type.TR_ACCESS_RIGHT)
//    private String accessRights;

    /**
     * The version date for the most recently published or broadcast resource.
     */
    @XmlElement(required = true)
    @Schema(example = "2020-01-01", required = true)
    @FieldValidation
    private Date versionDate;


    // Learning Information
    /**
     * The principal users(s) for which the learning resource was designed.
     */
    @XmlElementWrapper(name = "targetGroups", required = true)
    @XmlElement(name = "targetGroup")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TARGET_USER)
    private List<String> targetGroups;

    /**
     * The predominant type or kind that characterizes the learning resource.
     */
    @XmlElementWrapper(name = "learningResourceTypes")
    @XmlElement(name = "learningResourceType")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TR_DCMI_TYPE)
    private List<String> learningResourceTypes;

    /**
     * The descriptions of what knowledge, skills or abilities students should acquire on completion of the resource.
     */
    @XmlElementWrapper(name = "learningOutcomes", required = true)
    @XmlElement(name = "learningOutcome")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private List<String> learningOutcomes;

    /**
     * Target skill level in the topic being taught.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TR_EXPERTISE_LEVEL)
    private String expertiseLevel;

    /**
     * The predominant content type of the learning resource (video, game, diagram, slides, etc.).
     */
    @XmlElementWrapper(name = "contentResourceTypes")
    @XmlElement(name = "contentResourceType")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TR_CONTENT_RESOURCE_TYPE)
    private List<String> contentResourceTypes;

    /**
     * Identification of certification, accreditation or badge obtained with course or learning resource.
     */
    @XmlElementWrapper(name = "qualifications")
    @XmlElement(name = "qualification")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.TR_QUALIFICATION)
    private List<String> qualifications;

    /**
     * Approximate or typical time it takes to work with or through the learning resource for the typical intended target audience.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String duration;


    // Geographical and Language Availability Information
    /**
     * The language in which the resource was originally published or made available.
     */
    @XmlElementWrapper(name = "languages", required = true)
    @XmlElement(name = "language")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.LANGUAGE)
    private List<String> languages;


    // Classification Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @XmlElementWrapper(name = "scientificDomains", required = true)
    @XmlElement(name = "scientificDomain")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
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
     * The URL to the location of the actual tool.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String path;
    
    // TRL 
    /**
     * The Technology Readiness Level of the Resource (to be further updated in the context of the EOSC).
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String trl;
    
    // Github page 
    /**
     * The Github repository to find more information about this Resource.
     */
    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private String githubPage;
    
    // Attribution Information
    /**
     * Name of the funding body that supported the development and/or operation of the Resource.
     */
    @XmlElementWrapper(name = "fundingBody")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.FUNDING_BODY)
    private List<String> fundingBody;

    /**
     * Name of the funding program that supported the development and/or operation of the Resource.
     */
    @XmlElementWrapper(name = "fundingPrograms")
    @XmlElement(name = "fundingProgram")
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.FUNDING_PROGRAM)
    private List<String> fundingPrograms;

    /**
     * Name of the project that supported the development and/or operation of the Resource.
     */
    @XmlElementWrapper(name = "grantProjectNames")
    @XmlElement(name = "grantProjectName")
    @Schema
    @FieldValidation(nullable = true)
    private List<String> grantProjectNames;
    
    // Contact Information
    /**
     * Tool's Main Contact Owner info.
     */
    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private ServiceMainContact contact;



    public Tool() {
    }

    public Tool(String id, String name, String resourceOrganisation, List<String> resourceProviders, List<String> authors, URL url, String urlType, List<String> eoscRelatedServices, List<AlternativeIdentifier> alternativeIdentifiers, String description, List<String> keywords, String license, Date versionDate, List<String> targetGroups, List<String> learningResourceTypes, List<String> learningOutcomes, String expertiseLevel, List<String> contentResourceTypes, List<String> qualifications, String duration, List<ServiceProviderDomain> scientificDomains, ServiceMainContact contact) {
        this.id = id;
        this.name = name;
        this.resourceOrganisation = resourceOrganisation;
        this.resourceProviders = resourceProviders;
        this.authors = authors;
        this.description = description;
        this.keywords = keywords;
        this.license = license;
        this.versionDate = versionDate;
        this.targetGroups = targetGroups;
        this.learningResourceTypes = learningResourceTypes;
        this.learningOutcomes = learningOutcomes;
        this.expertiseLevel = expertiseLevel;
        this.contentResourceTypes = contentResourceTypes;
        this.qualifications = qualifications;
        this.duration = duration;
        this.scientificDomains = scientificDomains;
        this.contact = contact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tool that = (Tool) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(resourceOrganisation, that.resourceOrganisation) && Objects.equals(resourceProviders, that.resourceProviders) && Objects.equals(authors, that.authors) && Objects.equals(description, that.description) && Objects.equals(keywords, that.keywords) && Objects.equals(license, that.license) && Objects.equals(versionDate, that.versionDate) && Objects.equals(targetGroups, that.targetGroups) && Objects.equals(learningResourceTypes, that.learningResourceTypes) && Objects.equals(learningOutcomes, that.learningOutcomes) && Objects.equals(expertiseLevel, that.expertiseLevel) && Objects.equals(contentResourceTypes, that.contentResourceTypes) && Objects.equals(qualifications, that.qualifications) && Objects.equals(duration, that.duration) && Objects.equals(scientificDomains, that.scientificDomains) && Objects.equals(contact, that.contact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, resourceOrganisation, resourceProviders, authors, description, keywords, license, versionDate, targetGroups, learningResourceTypes, learningOutcomes, expertiseLevel, contentResourceTypes, qualifications, duration, languages, scientificDomains, contact);
    }

    @Override
    public String toString() {
        return "TrainingResource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", resourceOrganisation='" + resourceOrganisation + '\'' +
                ", resourceProviders=" + resourceProviders +
                ", authors=" + authors +
                ", description='" + description + '\'' +
                ", keywords=" + keywords +
                ", license='" + license + '\'' +
                ", versionDate=" + versionDate +
                ", targetGroups=" + targetGroups +
                ", learningResourceTypes=" + learningResourceTypes +
                ", learningOutcomes=" + learningOutcomes +
                ", expertiseLevel='" + expertiseLevel + '\'' +
                ", contentResourceTypes=" + contentResourceTypes +
                ", qualifications=" + qualifications +
                ", scientificDomains=" + scientificDomains +
                ", contact=" + contact +
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

    public List<String> getResourceProviders() {
        return resourceProviders;
    }

    public void setResourceProviders(List<String> resourceProviders) {
        this.resourceProviders = resourceProviders;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
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

    public List<String> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(List<String> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public List<String> getLearningResourceTypes() {
        return learningResourceTypes;
    }

    public void setLearningResourceTypes(List<String> learningResourceTypes) {
        this.learningResourceTypes = learningResourceTypes;
    }

    public List<String> getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(List<String> learningOutcomes) {
        this.learningOutcomes = learningOutcomes;
    }

    public String getExpertiseLevel() {
        return expertiseLevel;
    }

    public void setExpertiseLevel(String expertiseLevel) {
        this.expertiseLevel = expertiseLevel;
    }

    public List<String> getContentResourceTypes() {
        return contentResourceTypes;
    }

    public void setContentResourceTypes(List<String> contentResourceTypes) {
        this.contentResourceTypes = contentResourceTypes;
    }

    public List<String> getQualifications() {
        return qualifications;
    }

    public void setQualifications(List<String> qualifications) {
        this.qualifications = qualifications;
    }

    public List<ServiceProviderDomain> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }

    public ServiceMainContact getContact() {
        return contact;
    }

    public void setContact(ServiceMainContact contact) {
        this.contact = contact;
    }
}