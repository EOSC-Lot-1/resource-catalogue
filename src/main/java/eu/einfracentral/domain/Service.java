package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.util.List;

/**
 * Created by pgl on 29/6/2017.
 */
@XmlType(namespace = "http://einfracentral.eu", propOrder = {"id", "brandName", "tagline", "fullName", "description",
    "options", "targetUsers", "userValue", "userBase", "provider", "fundingSources", "webpage", "symbol",
    "multimediaURL", "version", "revisionDate", "versionHistory", "phase", "technologyReadinessLevel", "category",
    "subcategory", "countries", "regions", "languages", "tags", "relatedServices", "request", "helpdesk",
    "documentation", "trainingInformation", "feedback", "pricingModel", "serviceLevelAgreement", "termsOfUse"})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement (namespace = "http://einfracentral.eu" )
public class Service {

    //Basic
    /**
     * Global unique and persistent identifier of a specific service. Work in progress.
     */
    @XmlElement(required = true)
    private String id; //list

    /**
     * Brief marketing name of service as assigned by the service provider. Should be descriptive from a customer point of view, and should be quite simple, such that someone non-technical is able to understand what the service is about.
     */
    @XmlElement
    private String brandName;

    /**
     * Catchline or slogan of service for marketing/advertising  purposes.
     */
    @XmlElement(required = true)
    private String tagline;

    /**
     * Extended name of service as assigned by the service provider.
     */
    @XmlElement(required = true)
    private String fullName;

    /**
     * High-level description of what the service does in terms of functionality it provides and the resources it enables access to. Should be similar to the name described above, and should cover the value provided by the service, in fairly non-technical terms. These descriptions may seem obvious but help everyone within the organization understand the service, and also will be needed for the Service Catalogue, which will be shown to users and customers. It may provide also information related to the offered capacity, number of installations, underlying data that is offered.
     */
    @XmlElement(required = true)
    private String description;

    /**
     * A choice of utility and warranty that the customer can/should specify when commissioning the service
     */
    @XmlElement
    private String options;

    /**
     * Type of users or end-users allowed to commission/benefit from the service.
     */
    @XmlElement
    private String targetUsers; //may become list

    /**
     * The benefit to a customer and their users delivered by the service. Benefits are usually related to alleviating pains (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving).
     */
    @XmlElement
    private String userValue;

    /**
     * List of customers, communities, etc using the service.
     */
    @XmlElement
    private String userBase;

    /**
     * Organisation that manages and delivers the service and with whom the customer signs the SLA.
     */
    @XmlElement(required = true)
    private String provider; //may become list

    /**
     * Sources of funding for the development and operation of the service.
     */
    @XmlElement
    private String fundingSources;

    /**
     * Link to a webpage providing information about the service. This webpage is usually hosted and maintained by the service provider. It contains fresh and additional information, such as what APIs are supported or links to the documentation.
     */
    @XmlElement(required = true)
    private URL webpage;

    /**
     * Link to a visual representation for the service
     */
    @XmlElement(required = true)
    private URL symbol;

    /**
     * Link to a page containing multimedia regarding the service
     */
    @XmlElement
    private URL multimediaURL;

    //Classification
    /**
     * Informs about the implementation of the service that is in force as well as about its previous implementations, if any.
     */
    @XmlElement
    private String version;

    /**
     * The date of the latest update.
     */
    @XmlElement
    private XMLGregorianCalendar revisionDate;

    /**
     * A list of the service features added in the latest version
     */
    @XmlElement
    private String versionHistory;

    /**
     * Is used to tag the service to the full service cycle: e.g., discovery, alpha (prototype available for closed set of users), beta (service being developed while available for testing publicly), production, retired (not anymore offered).
     */
    @XmlElement(required = true)
    private String phase; //alpha, beta, production

    /**
     * Is used to tag the service to the Technology Readiness Level.
     */
    @XmlElement(required = true)
    private String technologyReadinessLevel; //7, 8 , 9

    /**
     * A named group of services that offer access to the same type of resource. These are external ones that are of interest to a customer.
     */
    @XmlElement(required = true)
    private String category; //e.g. storage, compute, networking, data, training, consultancy, etc.

    /**
     * Type of service within a category
     */
    @XmlElement(required = true)
    private String subcategory; //list

    /**
     * List of countries within which the service is available
     */
    //@XmlElementWrapper(required = true)
    @XmlElement(name = "country")
    private List<String> countries;

    /**
     * List of regions within which the service is available
     */
    //@XmlElementWrapper(required = true)
    @XmlElement(name = "region")
    private List<String> regions;

    /**
     * List of languages in which the service is available
     */
    //@XmlElementWrapper(required = true)
    @XmlElement(name = "language")
    private List<String> languages;

    /**
     * Field to facilitate searching based on keywords
     */
    //@XmlElementWrapper(required = true)
    @XmlElement(name = "tag")
    private List<String> tags;

    /**
     * Other services that are either required or commonly used with this service.
     */
    //@XmlElementWrapper(required = true)
    @XmlElement(name = "relatedService")
    private List<String> relatedServices;

    //Support
    /**
     * Link to request the service from the service provider
     */
    @XmlElement(required = true)
    private URL request;

    /**
     * Link with contact to ask more information from the service provider about this service. A contact person or helpdesk within the organization must be assigned for communications, questions and issues relating to the service.
     */
    @XmlElement
    private URL helpdesk;

    /**
     * Link to user manual and documentation
     */
    @XmlElement
    private URL documentation;

    /**
     * Link to training information
     */
    @XmlElement
    private URL trainingInformation;

    /**
     * Link to page where customers can provide feedback on the service
     */
    @XmlElement
    private URL feedback;

    //Contractual
    /**
     * Supported payment models that apply. List of sentences each of them stating the type of payment model and the restriction that applies to it.
     */
    @XmlElement(required = true)
    private URL pricingModel;

    /**
     * Document containing information about the levels of performance that a service provider is expected to achieve. Current service agreements (SLAs) available for the service or basis for a new SLA. These should be agreements with users (not providers).
     */
    @XmlElement
    private String serviceLevelAgreement;

    /**
     * Document containing the rules, service conditions and usage policy which one must agree to abide by in order to use the service.
     */
    @XmlElement
    private String termsOfUse;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(String targetUsers) {
        this.targetUsers = targetUsers;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public String getUserBase() {
        return userBase;
    }

    public void setUserBase(String userBase) {
        this.userBase = userBase;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFundingSources() {
        return fundingSources;
    }

    public void setFundingSources(String fundingSources) {
        this.fundingSources = fundingSources;
    }

    public URL getWebpage() {
        return webpage;
    }

    public void setWebpage(URL webpage) {
        this.webpage = webpage;
    }

    public URL getSymbol() {
        return symbol;
    }

    public void setSymbol(URL symbol) {
        this.symbol = symbol;
    }

    public URL getMultimediaURL() {
        return multimediaURL;
    }

    public void setMultimediaURL(URL multimediaURL) {
        this.multimediaURL = multimediaURL;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public XMLGregorianCalendar getRevisionDate() {
        return revisionDate;
    }

    public void setRevisionDate(XMLGregorianCalendar revisionDate) {
        this.revisionDate = revisionDate;
    }

    public String getVersionHistory() {
        return versionHistory;
    }

    public void setVersionHistory(String versionHistory) {
        this.versionHistory = versionHistory;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getTechnologyReadinessLevel() {
        return technologyReadinessLevel;
    }

    public void setTechnologyReadinessLevel(String technologyReadinessLevel) {
        this.technologyReadinessLevel = technologyReadinessLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public List<String> getCountries() {
        return countries;
    }

    public void setCountries(List<String> countries) {
        this.countries = countries;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getRelatedServices() {
        return relatedServices;
    }

    public void setRelatedServices(List<String> relatedServices) {
        this.relatedServices = relatedServices;
    }

    public URL getRequest() {
        return request;
    }

    public void setRequest(URL request) {
        this.request = request;
    }

    public URL getHelpdesk() {
        return helpdesk;
    }

    public void setHelpdesk(URL helpdesk) {
        this.helpdesk = helpdesk;
    }

    public URL getDocumentation() {
        return documentation;
    }

    public void setDocumentation(URL documentation) {
        this.documentation = documentation;
    }

    public URL getTrainingInformation() {
        return trainingInformation;
    }

    public void setTrainingInformation(URL trainingInformation) {
        this.trainingInformation = trainingInformation;
    }

    public URL getFeedback() {
        return feedback;
    }

    public void setFeedback(URL feedback) {
        this.feedback = feedback;
    }

    public URL getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(URL pricingModel) {
        this.pricingModel = pricingModel;
    }

    public String getServiceLevelAgreement() {
        return serviceLevelAgreement;
    }

    public void setServiceLevelAgreement(String serviceLevelAgreement) {
        this.serviceLevelAgreement = serviceLevelAgreement;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

}
