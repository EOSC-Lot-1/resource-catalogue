package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class DatasourceBundle extends Bundle<Datasource> {

    @XmlElement
    @VocabularyValidation(type = Vocabulary.Type.RESOURCE_STATUS)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    private String status;

    /**
     * Original OpenAIRE ID, if Datasource already exists in the OpenAIRE Catalogue
     */
    @XmlElement
    private String originalOpenAIREId;

    @XmlElement
    private boolean softwareRepository;
    
    @XmlElement()
    @FieldValidation(nullable = true)
    private Boolean resubmit;
    
    @XmlElement()
    @Schema
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.DS_TYPE)
    private String datasourceType;
    
    public DatasourceBundle() {
        // No arg constructor
    }

    public DatasourceBundle(Datasource datasource) {
        this.setDatasource(datasource);
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, Metadata metadata) {
        this.setDatasource(datasource);
        this.setMetadata(metadata);
    }

    public DatasourceBundle(Datasource datasource, String status) {
        this.setDatasource(datasource);
        this.status = status;
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, String status, String originalOpenAIREId) {
        this.setDatasource(datasource);
        this.status = status;
        this.originalOpenAIREId = originalOpenAIREId;
        this.setMetadata(null);
    }

    public DatasourceBundle(String status, String originalOpenAIREId, boolean softwareRepository) {
        this.status = status;
        this.originalOpenAIREId = originalOpenAIREId;
        this.softwareRepository = softwareRepository;
        this.setMetadata(null);
    }

    @Override
    public String toString() {
        return "DatasourceBundle{" +
                "status='" + status + '\'' +
                ", originalOpenAIREId='" + originalOpenAIREId + '\'' +
                ", softwareRepository=" + softwareRepository +
                '}';
    }

    @XmlElement(name = "datasource")
    public Datasource getDatasource() {
        return this.getPayload();
    }

    public void setDatasource(Datasource datasource) {
        this.setPayload(datasource);
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalOpenAIREId() {
        return originalOpenAIREId;
    }

    public void setOriginalOpenAIREId(String originalOpenAIREId) {
        this.originalOpenAIREId = originalOpenAIREId;
    }

    public boolean isSoftwareRepository() {
        return softwareRepository;
    }

    public void setSoftwareRepository(boolean softwareRepository) {
        this.softwareRepository = softwareRepository;
    }
    
    public Boolean getResubmit() {
        return resubmit;
    }

    public void setResubmit(Boolean resubmit) {
        this.resubmit = resubmit;
    }

}
