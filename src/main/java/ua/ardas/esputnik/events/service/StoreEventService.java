package ua.ardas.esputnik.events.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.dao.cassandra.errors.CassandraError;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.events.repository.EventBucketsCassandraDao;
import ua.ardas.esputnik.events.repository.EventByTypeRepository;
import ua.ardas.esputnik.events.repository.EventCampaignByTypeRepository;
import ua.ardas.esputnik.events.repository.entities.EventByType;
import ua.ardas.esputnik.events.repository.entities.EventByTypeKey;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByType;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByTypeKey;
import ua.ardas.esputnik.events.repository.entities.EventLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreEventService {

    private final EventByTypeRepository eventByTypeRepository;
    private final EventCampaignByTypeRepository eventCampaignByTypeRepository;
    private final EventBucketsCassandraDao bucketsCassandraDao;

    public void storeEventLog(EventRedisDto event, Integer campaignId) throws InterruptedException, CassandraError {
        long start = System.currentTimeMillis();
        EventLog eventLog = mapToEventLog(event, campaignId);
        if (null != campaignId) {
            eventCampaignByTypeRepository.insert(mapToEventCampaignByType(eventLog, campaignId));
        }
        bucketsCassandraDao.storeEventByType(eventLog);
        bucketsCassandraDao.storeEventByDivision(eventLog);
        eventByTypeRepository.insert(mapToEventByType(eventLog));

        if (log.isDebugEnabled()) {
            log.debug("Event store processed. eventTypeId: {}, elapsed: {}, elapsedAll: {}",
                    event.getEventTypeId(), System.currentTimeMillis() - start,
                    System.currentTimeMillis() - event.getDate().getTime());
        }
    }

    private EventLog mapToEventLog(EventRedisDto event, Integer campaignId) {
        return EventLog.builder()
                .eventId(UUID.randomUUID().toString())
                .date(event.getDate())
                .actions(campaignId == null ? null : String.valueOf(campaignId))
                .organisationId(event.getOrganisationId())
                .eventTypeId(event.getEventTypeId())
                .keyValue(event.getKeyValue())
                .params(event.getJson())
                .build();

    }

    private EventByType mapToEventByType(EventLog eventLog) {
        return new EventByType(new EventByTypeKey(eventLog.getEventTypeId(), eventLog.getKeyValue()), eventLog.getDate());
    }

    private EventCampaignByType mapToEventCampaignByType(EventLog eventLog, int campaignId) {
        return new EventCampaignByType(
                new EventCampaignByTypeKey(eventLog.getEventTypeId(), campaignId, eventLog.getKeyValue()), eventLog.getDate());
    }
}
