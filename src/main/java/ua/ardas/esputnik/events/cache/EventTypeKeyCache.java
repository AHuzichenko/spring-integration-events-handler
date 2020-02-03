package ua.ardas.esputnik.events.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.cache.LoggingRemovalListener;
import ua.ardas.esputnik.cache.TestAwareCacheBuilder;
import ua.ardas.esputnik.commons.dao.procedures.event.EventCategory;
import ua.ardas.esputnik.commons.dao.procedures.event.EventTypeSelectOrCreateByKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventTypeKeyCache {

	private static final int MAXIMUM_CASH_SIZE = 50_000;

	@Value("${events.cache.expiration.time.sec:600}")
	private long cacheExpireSeconds;
	private LoadingCache<Pair<Integer, String>, Integer> cache;

	private final EventTypeSelectOrCreateByKey selectOrCreateByKey;

	@PostConstruct
	private void init() {
		log.debug("Init EventTypeIdByKeyCache cache");
		cache = TestAwareCacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_CASH_SIZE)
				.refreshAfterWrite(cacheExpireSeconds, TimeUnit.SECONDS)
				.asyncRefresh()
				.removalListener(new LoggingRemovalListener<Pair<Integer, String>, Integer>())
				.build(new EventLoader());
	}

	public int getEventTypeId(String eventTypeKey, int organisationId) throws ExecutionException {
		return cache.get(Pair.of(organisationId, eventTypeKey));
	}

	private class EventLoader extends CacheLoader<Pair<Integer, String>, Integer> {

		@Override
		public Integer load(Pair<Integer, String> params) {
			return selectOrCreateByKey.run(params.getValue(), params.getKey(), EventCategory.CUSTOM.getId());
		}
	}
}