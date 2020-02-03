package ua.ardas.esputnik.events.cache;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ua.ardas.esputnik.cache.TestAwareCacheBuilder;
import ua.ardas.esputnik.events.domain.Campaign;
import ua.ardas.esputnik.events.repository.CampaignRepository;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignsCache {

    private static final int MAX_SIZE = 50_000;

    @Value("${events.cache.expiration.time.sec:600}")
    private long cacheExpireSeconds;
    private LoadingCache<Integer, Campaign> cache;

    private final CampaignRepository campaignRepository;

    @PostConstruct
    private void postConstruct() {
        cache = TestAwareCacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(cacheExpireSeconds, TimeUnit.SECONDS)
                .build(new CampaignsLoader());
    }

    public Campaign get(int campaignId) {
        try {
            return cache.get(campaignId);
        } catch (ExecutionException e) {
            log.error("Cannot get campaign: id = {}", campaignId, e);
        }
        return null;
    }

    private class CampaignsLoader extends CacheLoader<Integer, Campaign> {

        @Override
        @ParametersAreNonnullByDefault
        public Campaign load(Integer campaignId) {
            return campaignRepository.getCampaign(campaignId);
        }
    }
}