package gr.uoa.di.madgik.resourcecatalogue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The resources owned by a Provider, grouped by type.
 * Types that were not requested are null and omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderResources {

    @Schema(description = "Services of the Provider that have no Datasource")
    private List<ServiceBundle> services;

    @Schema(description = "Datasources of the Provider's Services")
    private List<DatasourceBundle> datasources;

    @Schema(description = "Training Resources of the Provider")
    private List<TrainingResourceBundle> trainingResources;

    @Schema(description = "Interoperability Records of the Provider")
    private List<InteroperabilityRecordBundle> interoperabilityRecords;

    @Schema(description = "Tools of the Provider")
    private List<ToolBundle> tools;

    public ProviderResources() {
    }

    public List<ServiceBundle> getServices() {
        return services;
    }

    public void setServices(List<ServiceBundle> services) {
        this.services = services;
    }

    public List<DatasourceBundle> getDatasources() {
        return datasources;
    }

    public void setDatasources(List<DatasourceBundle> datasources) {
        this.datasources = datasources;
    }

    @JsonProperty("training-resources")
    public List<TrainingResourceBundle> getTrainingResources() {
        return trainingResources;
    }

    @JsonProperty("training-resources")
    public void setTrainingResources(List<TrainingResourceBundle> trainingResources) {
        this.trainingResources = trainingResources;
    }

    @JsonProperty("interoperability-records")
    public List<InteroperabilityRecordBundle> getInteroperabilityRecords() {
        return interoperabilityRecords;
    }

    @JsonProperty("interoperability-records")
    public void setInteroperabilityRecords(List<InteroperabilityRecordBundle> interoperabilityRecords) {
        this.interoperabilityRecords = interoperabilityRecords;
    }

    public List<ToolBundle> getTools() {
        return tools;
    }

    public void setTools(List<ToolBundle> tools) {
        this.tools = tools;
    }
}
