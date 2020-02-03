package ua.ardas.esputnik.events.service;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.ardas.esputnik.commons.monitoring.metrics.Monitoring;
import ua.ardas.esputnik.dao.cassandra.errors.CassandraError;
import ua.ardas.esputnik.events.cache.EventActionCache;
import ua.ardas.esputnik.events.cache.EventTypeKeyCache;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;
import ua.ardas.esputnik.utils.ToStringUtils;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {

    private static final Counter processedCounter = Monitoring.GLOBAL_REGISTRY.counter("events-handler.events.processed");

    private final EventActionCache eventActionCache;
    private final EventTypeKeyCache eventTypeCache;
    private final CampaignStartService campaignService;
    private final CopyEventService copyEventService;
    private final StoreEventService storeEventService;
    private final EventStrategyService eventStrategyService;

    public void processEvents(EventRedisDto event) throws InterruptedException, CassandraError, ExecutionException {
        try {
            if (null != event.getEventTypeKey()) {
                event.setEventTypeId(eventTypeCache.getEventTypeId(event.getEventTypeKey(), event.getOrganisationId()));
            }
            if (log.isTraceEnabled()) {
                log.trace("Process event: {}", ToStringUtils.toStringMultilineJson(event));
            } else if (log.isDebugEnabled()) {
                log.debug("Process event. eventTypeId: {}, keyValue: {}", event.getEventTypeId(), event.getKeyValue());
            }

            EventAction eventAction = eventActionCache.getByEventTypeId(event.getEventTypeId());

            Integer campaignId = null;
            if (null != eventAction) {
                campaignId = processAction(event, eventAction);
            } else {
                log.debug("Action for eventTypeId: {} is not set", event.getEventTypeId());
            }

            processedCounter.increment();

            storeEventService.storeEventLog(event, campaignId);
        } finally {
            copyEventService.copyEvent(event);
        }
    }

    private Integer processAction(EventRedisDto event, EventAction activeCampaign) {
        try {
            if (eventStrategyService.checkStrategy(event, activeCampaign) && checkPermissions(event, activeCampaign)) {
                campaignService.startCampaign(activeCampaign, event);
                return activeCampaign.getCampaignId();
            } else {
                log.debug("Skip event processing");
            }
        }
        catch (NoSuchElementException e) {
            log.warn("No active campaigns for eventTypeId: {}", event.getEventTypeId());
        }
        catch (RetriableException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Cannot start campaign: {}", e.getMessage(), e);
        }
        return null;
    }

    private boolean checkPermissions(EventRedisDto event, EventAction activeCampaign) {
        if (activeCampaign.getOrganisationId() != event.getOrganisationId()) {
            log.warn("eventTypeId and generated event have different orgIds: eventTypeId={}, orgIds: {} and {}",
                    event.getEventTypeId(), activeCampaign.getOrganisationId(), event.getOrganisationId());
            return false;
        }
        return true;
    }
}
