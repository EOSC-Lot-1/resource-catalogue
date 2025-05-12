package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class NodeInfo {
	
	@XmlElement()
	@Schema(description = "Public")
	@FieldValidation(nullable = true)
	private Boolean isNode;


	@XmlElement()
	@Schema(description = "Public")
	@FieldValidation(nullable = true)
	private String openAIRE_community_tag;
	
    @XmlElement
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private EnrollementSteps enrollementSteps;
    
	public NodeInfo() {	
	}
    
	public NodeInfo(Boolean isNode, String openAIRE_community_tag, EnrollementSteps enrollementSteps) {
		this.isNode = isNode;
		this.openAIRE_community_tag = openAIRE_community_tag;
		this.enrollementSteps = enrollementSteps;
	}

	public Boolean getIsNode() {
		return isNode;
	}

	public void setIsNode(Boolean isNode) {
		this.isNode = isNode;
	}

	public String getOpenAIRE_community_tag() {
		return openAIRE_community_tag;
	}

	public void setOpenAIRE_community_tag(String openAIRE_community_tag) {
		this.openAIRE_community_tag = openAIRE_community_tag;
	}

	public EnrollementSteps getEnrollementSteps() {
		return enrollementSteps;
	}

	public void setEnrollementSteps(EnrollementSteps enrollementSteps) {
		this.enrollementSteps = enrollementSteps;
	}
}
