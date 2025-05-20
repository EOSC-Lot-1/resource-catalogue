package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
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
	@Schema(description = "Private")
	@FieldValidation(nullable = true)
	private String openAIRECommunityTag;
	
	@XmlElement()
    @Schema(description = "Private")
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.NODE_TYPE)
    private String nodeType;
	
    @XmlElement
    @Schema(description = "Private")
    @FieldValidation(nullable = true)
    private EnrollmentSteps enrollmentSteps;
    
	public NodeInfo() {	
	}
    
	public NodeInfo(Boolean isNode, String openAIRECommunityTag, String nodeType, EnrollmentSteps enrollmentSteps) {
		this.isNode = isNode;
		this.openAIRECommunityTag = openAIRECommunityTag;
		this.nodeType = nodeType;
		this.enrollmentSteps = enrollmentSteps;
	}

	public Boolean getIsNode() {
		return isNode;
	}

	public void setIsNode(Boolean isNode) {
		this.isNode = isNode;
	}

	public String getOpenAIRECommunityTag() {
		return openAIRECommunityTag;
	}

	public void setOpenAIRECommunityTag(String openAIRECommunityTag) {
		this.openAIRECommunityTag = openAIRECommunityTag;
	}
	
	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public EnrollmentSteps getEnrollmentSteps() {
		return enrollmentSteps;
	}

	public void setEnrollmentSteps(EnrollmentSteps enrollmentSteps) {
		this.enrollmentSteps = enrollmentSteps;
	}
}
