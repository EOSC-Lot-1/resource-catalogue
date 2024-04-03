package gr.uoa.di.madgik.resourcecatalogue.config;

import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.*;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger logger = LogManager.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(

                new ConcurrentMapCache(CACHE_VISITS,
                        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_FEATURED,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(5).build().asMap(), false),
                new ConcurrentMapCache(CACHE_PROVIDERS,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(10).build().asMap(), false),
                new ConcurrentMapCache(CACHE_EVENTS,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(1000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_SERVICE_EVENTS,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(1000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_VOCABULARIES,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50).build().asMap(), false),
                new ConcurrentMapCache(CACHE_VOCABULARY_MAP,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50).build().asMap(), false),
                new ConcurrentMapCache(CACHE_VOCABULARY_TREE,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50).build().asMap(), false),

                // NEEDED FOR registry-core
                new ConcurrentMapCache("resourceTypes"),
                new ConcurrentMapCache("resourceTypesIndexFields")
        ));
        return cacheManager;
    }

    //    @Scheduled(initialDelay = 0, fixedRate = 120000) //run every 2 min
    @Scheduled(cron = "0 0 12 ? * *") // At 12:00:00pm every day
    public void updateCache() throws IOException, InterruptedException {
        // Update Cache URL
        URL updateCache = new URL("https://providers.eosc-portal.eu/stats-api/cache/updateCache");
        HttpURLConnection updateCon = (HttpURLConnection) updateCache.openConnection();
        updateCon.setRequestMethod("GET");
        // Promote Cache URL
        URL promoteCache = new URL("https://providers.eosc-portal.eu/stats-api/cache/promoteCache");
        HttpURLConnection promoteCon = (HttpURLConnection) promoteCache.openConnection();
        promoteCon.setRequestMethod("GET");
        int responseUpdateCode = updateCon.getResponseCode();
        logger.info(String.format("Updating Cache. Response Code: %d", responseUpdateCode));
        if (responseUpdateCode == HttpURLConnection.HTTP_OK) { // success
            logger.info("Success..Proceeding to Promoting Cache");
            TimeUnit.MINUTES.sleep(1);
            int responsePromoteCode = promoteCon.getResponseCode();
            logger.info(String.format("Promoting Cache. Response Code: %d", responsePromoteCode));
            if (responsePromoteCode == HttpURLConnection.HTTP_OK) { // success
                logger.info("Cache Updated and Promoted Successfully");
            } else {
                logger.info(String.format("An error occurred while trying to Promote Cache. Response Code: %d", responsePromoteCode));
            }
        } else {
            logger.info(String.format("An error occurred while trying to Update Cache. Response Code: %d", responseUpdateCode));
        }
    }
}
