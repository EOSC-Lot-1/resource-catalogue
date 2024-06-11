package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PIDService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@org.springframework.stereotype.Service("pidManager")
public class PIDManager implements PIDService {

    private static final Logger logger = LoggerFactory.getLogger(PIDManager.class);

    private final ProviderResourcesCommonMethods commonMethods;
    @Value("${pid.username}")
    private String pidUsername;
    @Value("${pid.auth}")
    private String pidAuth;
    @Value("${pid.api}")
    private String pidApi;
    @Value("${marketplace.url}")
    private String marketplaceUrl;

    public PIDManager(ProviderResourcesCommonMethods commonMethods) {
        this.commonMethods = commonMethods;
    }

    public Bundle<?> get(String pid, String resourceType) {
        return commonMethods.getResourceViaPID(pid, resourceType);
    }

    public void updatePID(String pid, String resourceTypePath) {
        String pidPrefix = commonMethods.determinePidPrefix(resourceTypePath);
        String url = pidApi + pid;
        String payload = formatPIDToIncludeMarketplaceUrl(pid, pidPrefix, resourceTypePath);
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            con.setRequestMethod("PUT");
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", pidAuth);
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            logger.info("Updating PID {} for resource with ID {}", pid, pid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatPIDToIncludeMarketplaceUrl(String resourceId, String pidPrefix, String resourceTypePath) {
        JSONObject data = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject hs_admin = new JSONObject();
        JSONObject hs_admin_data = new JSONObject();
        JSONObject hs_admin_data_value = new JSONObject();
        JSONObject id = new JSONObject();
        JSONObject marketplaceUrl = new JSONObject();
        hs_admin_data_value.put("index", 301);
        hs_admin_data_value.put("handle", pidPrefix + "/" + pidUsername);
        hs_admin_data_value.put("permissions", "011111110011");
        hs_admin_data_value.put("format", "admin");
        hs_admin_data.put("value", hs_admin_data_value);
        hs_admin_data.put("format", "admin");
        hs_admin.put("index", 100);
        hs_admin.put("type", "HS_ADMIN");
        hs_admin.put("data", hs_admin_data);
        values.put(hs_admin);
        marketplaceUrl.put("index", 1);
        marketplaceUrl.put("type", "url");
        String url = this.marketplaceUrl;
        if (resourceTypePath.equals("trainings/") || resourceTypePath.equals("guidelines/")) {
            url = url.replace("marketplace", "search.marketplace");
        }
        marketplaceUrl.put("data", url + resourceTypePath + resourceId);
        values.put(marketplaceUrl);
        id.put("index", 2);
        id.put("type", "id");
        id.put("data", resourceId);
        values.put(id);
        data.put("values", values);
        return data.toString();
    }

    @Override
    public String determineResourceTypePath(String resourceType) {
        return switch (resourceType) {
            case "service" -> "services/";
            case "tool" -> "tools/";
            case "training_resource" -> "trainings/";
            case "provider" -> "providers/";
            case "interoperability_record" -> "guidelines/";
            default -> "no_path";
        };
    }
}
