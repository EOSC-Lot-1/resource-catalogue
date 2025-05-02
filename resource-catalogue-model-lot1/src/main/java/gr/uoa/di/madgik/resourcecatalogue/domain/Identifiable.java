package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;

//BeanUtils.getProperty(resourceToAdd,"getId") to get id, if I ever drop Identifiable due to w/e error
public interface Identifiable {
	
	@Schema(description = "Public")
    String getId();
	
	@Schema(description = "Public")
    void setId(String s);
}
