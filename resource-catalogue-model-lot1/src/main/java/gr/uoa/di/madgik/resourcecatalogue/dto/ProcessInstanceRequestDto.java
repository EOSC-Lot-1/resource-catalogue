package gr.uoa.di.madgik.resourcecatalogue.dto;


import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;


public class ProcessInstanceRequestDto {

    /**
     * Optional unique user identifier.
     */
    @Schema(
        description =
        """
        Unique user identifier, set to the `sub` attribute of a valid access token. This value *MUST* be present when the
        request is performed on behalf of an authenticated user, i.e., when the sender service either has a valid user access
        token or knows the user `sub` attribute value. If the user identifier is not available, then the service OIDC client
        id should be used.
        """,
        example     = "5f373f4a-2245-4754-aea2-6f302d10f6b6@eosc-federation.eu"
    )
    private String userKey;

    /**
     * Unique business key for the workflow process instance.
     */
    @NotNull()
    @Schema(
        description = "Unique business key",
        example     = "d85c0817-c8b8-4b38-970a-e79604f350d5"
    )
    private String businessKey;

    /**
     * The process definition key.
     * <p>
     * The service expects that a deployment with a process definition with the
     * specified key exists.
     */
    @NotEmpty()
    @Schema(
        description = "Process definition key",
        example     = "account-registration"
    )
    private String processDefinitionKey;

    /**
     * A list with process instance variables. Optionally, the list may be empty.
     */
    @NotNull()
    @Schema()
    private List<ProcessInstanceRequestVariableDto> variables;
    
    public ProcessInstanceRequestDto(String userKey, String businessKey, String processDefinitionKey,
    		List<ProcessInstanceRequestVariableDto> variables) {
    		this.userKey = userKey;
    		this.businessKey = businessKey;
    		this.processDefinitionKey = processDefinitionKey;
    		this.variables = variables;
    }
    
    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }
    
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey= businessKey;
    }
    
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setUProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }
    
    public List<ProcessInstanceRequestVariableDto> getVariables() {
        return variables;
    }

    public void setBusinessKey(List<ProcessInstanceRequestVariableDto> variables) {
        this.variables= variables;
    }

}