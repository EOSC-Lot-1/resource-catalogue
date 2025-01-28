package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Site {


    @XmlElement()
    @Schema
    //@FieldValidation()
    private String name;
    
    /**
     * Endpoints of the service
     */
    @XmlElementWrapper(name = "endpoints")
    @XmlElement(name = "endpoints")
    @Schema
    @FieldValidation()
    private List<Endpoint> endpoints;

    public Site() {
    }

    public Site(String name, List<Endpoint> endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }

    @Override
    public String toString() {
        return "Site{" +
                ", name='" + name + '\'' +
                 ", endpoints='" + endpoints + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site that = (Site) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints);
    }
}
