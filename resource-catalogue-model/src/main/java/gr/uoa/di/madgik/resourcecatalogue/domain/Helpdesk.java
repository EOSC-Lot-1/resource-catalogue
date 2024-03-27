package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Helpdesk implements Identifiable {

    @XmlElement
    @ApiModelProperty(position = 1, notes = "Monitoring ID", example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, notes = "Service ID", required = true)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String serviceId;

    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    @ApiModelProperty(position = 3, notes = "free text")
    private List<String> services;

    @XmlElement
    @ApiModelProperty(position = 4, notes = "Unique identifier of the helpdesk type")
    private String helpdeskType;

    @XmlElementWrapper(name = "supportGroups")
    @XmlElement(name = "supportGroup")
    @ApiModelProperty(position = 5, notes = "Support group to be created in the helpdesk for the provider")
    private List<String> supportGroups;

    @XmlElement
    @ApiModelProperty(position = 6, notes = "Name of organisation")
    @FieldValidation(nullable = true)
    private String organisation;

    @XmlElementWrapper(name = "emails")
    @XmlElement(name = "email")
    @ApiModelProperty(position = 7, notes = "E-mail for direct assignment of the tickets, bypassing the L1 support")
    private List<String> emails;

    @XmlElementWrapper(name = "agents")
    @XmlElement(name = "agent")
    @ApiModelProperty(position = 8, notes = "Person involved in ticket management")
    private List<String> agents;

    @XmlElementWrapper(name = "signatures")
    @XmlElement(name = "signature")
    @ApiModelProperty(position = 9, notes = "Automatic signature to be used in the answers to the tickets")
    @FieldValidation(nullable = true)
    private List<String> signatures;

    @XmlElement
    @ApiModelProperty(position = 10, notes = "Should the tickets be stored in the helpdesk system in dedicated group")
    @FieldValidation(nullable = true)
    private Boolean ticketPreservation;

    @XmlElement
    @ApiModelProperty(position = 11, notes = "Webform required to generate ticket directly on webpage")
    @FieldValidation(nullable = true)
    private Boolean webform;

    public Helpdesk() {
    }

    public Helpdesk(String id, String serviceId, List<String> services, String helpdeskType, List<String> supportGroups, String organisation, List<String> emails, List<String> agents, List<String> signatures, Boolean ticketPreservation, Boolean webform) {
        this.id = id;
        this.serviceId = serviceId;
        this.services = services;
        this.helpdeskType = helpdeskType;
        this.supportGroups = supportGroups;
        this.organisation = organisation;
        this.emails = emails;
        this.agents = agents;
        this.signatures = signatures;
        this.ticketPreservation = ticketPreservation;
        this.webform = webform;
    }

    public enum HelpdeskType {
        FULL_INTEGRATION("full integration"),
        TICKET_REDIRECTION("ticket redirection"),
        DIRECT_USAGE("direct usage");

        private final String helpdeskType;

        HelpdeskType(final String helpdeskType) {
            this.helpdeskType = helpdeskType;
        }

        public String getKey() {
            return helpdeskType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Helpdesk.HelpdeskType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Helpdesk.HelpdeskType.values())
                    .filter(v -> v.helpdeskType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown value [%s] found in field 'helpdeskType'. " +
                            "Available values: ['full integration', 'ticket redirection' and 'direct usage']", s)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Helpdesk helpdesk = (Helpdesk) o;
        return Objects.equals(id, helpdesk.id) && Objects.equals(serviceId, helpdesk.serviceId) && Objects.equals(services, helpdesk.services) && Objects.equals(helpdeskType, helpdesk.helpdeskType) && Objects.equals(supportGroups, helpdesk.supportGroups) && Objects.equals(organisation, helpdesk.organisation) && Objects.equals(emails, helpdesk.emails) && Objects.equals(agents, helpdesk.agents) && Objects.equals(signatures, helpdesk.signatures) && Objects.equals(ticketPreservation, helpdesk.ticketPreservation) && Objects.equals(webform, helpdesk.webform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serviceId, services, helpdeskType, supportGroups, organisation, emails, agents, signatures, ticketPreservation, webform);
    }

    @Override
    public String toString() {
        return "Helpdesk{" +
                "id='" + id + '\'' +
                ", serviceId=" + serviceId +
                ", services=" + services +
                ", helpdeskType='" + helpdeskType + '\'' +
                ", supportGroups=" + supportGroups +
                ", organisation='" + organisation + '\'' +
                ", emails=" + emails +
                ", agents=" + agents +
                ", signatures=" + signatures +
                ", ticketPreservation=" + ticketPreservation +
                ", webform=" + webform +
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public String getHelpdeskType() {
        return helpdeskType;
    }

    public void setHelpdeskType(String helpdeskType) {
        this.helpdeskType = helpdeskType;
    }

    public List<String> getSupportGroups() {
        return supportGroups;
    }

    public void setSupportGroups(List<String> supportGroups) {
        this.supportGroups = supportGroups;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getAgents() {
        return agents;
    }

    public void setAgents(List<String> agents) {
        this.agents = agents;
    }

    public List<String> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<String> signatures) {
        this.signatures = signatures;
    }

    public Boolean getTicketPreservation() {
        return ticketPreservation;
    }

    public void setTicketPreservation(Boolean ticketPreservation) {
        this.ticketPreservation = ticketPreservation;
    }

    public Boolean getWebform() {
        return webform;
    }

    public void setWebform(Boolean webform) {
        this.webform = webform;
    }
}
