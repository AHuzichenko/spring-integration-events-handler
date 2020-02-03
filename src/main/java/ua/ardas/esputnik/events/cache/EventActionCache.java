package ua.ardas.esputnik.events.cache;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.cache.TestAwareCacheBuilder;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.events.repository.EventActionRepository;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventActionCache {

    private static final int MAXIMUM_CASH_SIZE = 50_000;

    @Value("${events.cache.expiration.time.sec:600}")
    private long cacheExpireSeconds;
    private LoadingCache<Integer, List<EventAction>> cache;

    private final EventActionRepository eventActionSelect;

    @PostConstruct
    private void postConstruct() {
        cache = TestAwareCacheBuilder.newBuilder()
                .maximumSize(MAXIMUM_CASH_SIZE)
                .expireAfterWrite(cacheExpireSeconds, TimeUnit.SECONDS)
                .build(new EventActionLoader());
    }

    public EventAction getByEventTypeId(int eventTypeId) {
        try {
            List<EventAction> eventActions = cache.get(eventTypeId);
            return eventActions.stream()
                    .filter(eventAction -> !eventAction.isCampaignPaused() && eventAction.isCampaignActive())
                    .findFirst()
                    .orElse(null);
        } catch (ExecutionException | UncheckedExecutionException e) {
            log.error("Failed to get EventActions by Type Id {}.", eventTypeId, e);
            throw new RetriableException("Failed get EventActions.");
        }
    }

    public void clearCacheForSpecificEvent(Integer eventTypeId) {
        cache.invalidate(eventTypeId);
    }

    private class EventActionLoader extends CacheLoader<Integer, List<EventAction>> {

        @Override
        @ParametersAreNonnullByDefault
        public List<EventAction> load(Integer eventTypeId) {
            return eventActionSelect.find(eventTypeId);
        }
    }

}