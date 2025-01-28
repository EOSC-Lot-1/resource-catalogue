package gr.uoa.di.madgik.resourcecatalogue.service.sync;

import gr.uoa.di.madgik.resourcecatalogue.domain.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ToolSync extends AbstractSyncService<Tool> {

    @Autowired
    public ToolSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable}") boolean enabled) {
        super(host, filename, enabled);
    }

    @Override
    protected String getController() {
        return "/tool";
    }
}
