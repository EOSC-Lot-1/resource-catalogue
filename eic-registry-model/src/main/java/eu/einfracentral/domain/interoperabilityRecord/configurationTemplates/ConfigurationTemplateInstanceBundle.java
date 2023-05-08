package eu.einfracentral.domain.interoperabilityRecord.configurationTemplates;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstance;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ConfigurationTemplateInstanceBundle extends Bundle<ConfigurationTemplateInstance> {

    public ConfigurationTemplateInstanceBundle() {
    }

    public ConfigurationTemplateInstanceBundle(ConfigurationTemplateInstance configurationTemplateInstance) {
        this.setConfigurationTemplateInstance(configurationTemplateInstance);
        this.setMetadata(null);
    }

    public ConfigurationTemplateInstanceBundle(ConfigurationTemplateInstance configurationTemplateInstance, Metadata metadata) {
        this.setConfigurationTemplateInstance(configurationTemplateInstance);
        this.setMetadata(metadata);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @XmlElement(name = "configurationTemplateInstance")
    public ConfigurationTemplateInstance getConfigurationTemplateInstance() {
        return this.getPayload();
    }

    public void setConfigurationTemplateInstance(ConfigurationTemplateInstance configurationTemplateInstance) {
        this.setPayload(configurationTemplateInstance);
    }
}
