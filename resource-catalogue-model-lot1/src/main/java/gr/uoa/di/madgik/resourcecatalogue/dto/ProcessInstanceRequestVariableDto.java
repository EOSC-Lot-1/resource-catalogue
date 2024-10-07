package gr.uoa.di.madgik.resourcecatalogue.dto;



import javax.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProcessInstanceRequestVariableDto {

    @NotEmpty()
    @Schema(
        description = "Variable unique name",
        example     = "STATUS"
    )
    private String name;

    @NotEmpty()
    @Schema(
        description = "Variable value",
        example     = "SUCCESS"
    )
    private String value;

    /**
     * The variable type.
     * <p>
     * All variable values are of type {@code String}. This property provides
     * additional information about the variable's expected type but does not create
     * a Camunda {@code TypedValue} variable. A worker instance must parse the value
     * to the required type.
     */
    @NotEmpty()
    @Schema(
        description = "Variable type",
        example     = "BOOLEAN"
    )
    private EnumVariableType type;
    
    
    public ProcessInstanceRequestVariableDto(String name, String value, EnumVariableType type){
    	this.name 	= name;
    	this.value 	= value;
    	this.type 	= type;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public EnumVariableType getType() {
        return type;
    }

    public void setName(EnumVariableType type) {
        this.type= type;
    }
}