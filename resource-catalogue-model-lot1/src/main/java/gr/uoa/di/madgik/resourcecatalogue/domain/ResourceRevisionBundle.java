package gr.uoa.di.madgik.resourcecatalogue.domain;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

//@Document
@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceRevisionBundle extends Bundle<ResourceRevision> {
	
	@XmlElement
    @Schema(description = "Private")
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;
	
    @XmlElement
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
	private String originalId;
    
    // Only used for datasources
    @XmlElement
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
	private String linkedServiceRevisionId;

	@XmlElement
    @Schema(description = "Private")
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_TYPE)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String resourceType;

    public ResourceRevisionBundle() {
        // No arg constructor
    }

    public ResourceRevisionBundle(ResourceRevision resource) {
        this.setPayload(resource);
        this.setMetadata(null);
    }

    public ResourceRevisionBundle(ResourceRevision resource, Metadata metadata) {
        this.setPayload(resource);
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

    @XmlElement(name = "resource")
    public ResourceRevision getResourceRevision() {
        return this.getPayload();
    }

    public void setResourceRevision(ResourceRevision resource) {
        this.setPayload(resource);
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getOriginalId() {
		return originalId;
	}

	public void setOriginalId(String originalId) {
		this.originalId = originalId;
	}

	public String getLinkedServiceRevisionId() {
		return linkedServiceRevisionId;
	}

	public void setLinkedServiceRevisionId(String linkedServiceRevisionId) {
		this.linkedServiceRevisionId = linkedServiceRevisionId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

}



