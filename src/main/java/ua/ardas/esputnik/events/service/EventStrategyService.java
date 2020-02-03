package ua.ardas.esputnik.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.ardas.esputnik.events.repository.EventsCassandraDao;
import ua.ardas.esputnik.events.repository.entities.EventLog;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.events.domain.StrategyInterval;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.events.repository.EventCampaignByTypeRepository;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByType;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByTypeKey;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStrategyService {

    private final EventsCassandraDao cassandraDao;
    private final EventCampaignByTypeRepository eventCampaignByTypeRepository;

    @Value("#{new java.text.SimpleDateFormat('dd-MM-yyyy').parse('${events.hHour}')}")
    private Date hHour;

    public boolean checkStrategy(EventRedisDto event, EventAction eventAction) {
        if (null == eventAction) {
            return false;
        }

        Date startDate = getStartDate(eventAction);

        Optional<Date> eventDate = startDate.after(hHour)
                ? getEventLastDate(event, eventAction.getCampaignId())
                : getFiredEventLog(event);

        if (eventDate.isPresent() && StringUtils.isEmpty(eventAction.getStrategy())) {
            return false;
        }

        return !eventDate.isPresent() || startDate.after(eventDate.get());
    }

    private Optional<Date> getEventLastDate(EventRedisDto event, int campaignId) {
        log.debug("Getting events last date by event type {}, campaignId {} and key {}.", event.getEventTypeId(), campaignId,
                event.getKeyValue());
        return eventCampaignByTypeRepository.findById(
                new EventCampaignByTypeKey(event.getEventTypeId(), campaignId, event.getKeyValue()))
                .map(EventCampaignByType::getLastDate);
    }

    private Date getStartDate(EventAction eventAction) {
        try {
            if (StringUtils.isEmpty(eventAction.getStrategy())) {
                return new Date();
            }
            int amount = Integer.parseInt(eventAction.getStrategy());
            ChronoUnit interval = StrategyInterval.getIntervalByCode(eventAction.getInterval());
            return Date.from(LocalDateTime.now().minus(amount, interval).atZone(ZoneId.systemDefault()).toInstant());
        } catch (NumberFormatException e) {
            log.warn(String.format("Failed to check strategy: eventAction = %s", eventAction));
            return new Date();
        }
    }

    @Deprecated
    private Optional<Date> getFiredEventLog(EventRedisDto event) {
        long start = System.currentTimeMillis();
        Optional<Date> eventDate;
        log.debug("Getting events date (OLD way), for event type {} and key {}", event.getEventTypeId(), event.getKeyValue());
        try {
            eventDate =
                    Optional.ofNullable(cassandraDao.findLastEventAction(event.getEventTypeId(), event.getKeyValue())).map(EventLog::getDate);
        } catch (Exception e) {
            log.warn("Failed to find last event actions date :", e);
            throw new RetriableException(e);
        }
        log.debug("Last event date from cassandra: eventTypeId = {}, elapsed = {}", event.getEventTypeId(), System.currentTimeMillis() - start);

        return eventDate;
    }
}
