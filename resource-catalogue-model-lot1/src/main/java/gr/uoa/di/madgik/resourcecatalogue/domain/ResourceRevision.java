package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

public class ResourceRevision implements Identifiable {

	@XmlElement()
    @Schema(description = "Private")
	private String id; 
    
	@XmlElement()
    @Schema(description = "Private")
	private String resourceData; 

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String s) {
		this.id = s;
	}

	public String getResourceData() {
		return resourceData;
	}

	public void setResourceData(String resourceData) {
		this.resourceData = resourceData;
	}
}