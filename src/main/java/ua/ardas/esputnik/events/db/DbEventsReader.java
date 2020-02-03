package ua.ardas.esputnik.events.db;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.ardas.esputnik.commons.dao.dto.event.Event;
import ua.ardas.esputnik.commons.dao.procedures.event.GetNextEvents;
import ua.ardas.esputnik.events.queue.DbEventsQueue;
import ua.ardas.esputnik.events.queue.EventRedisDto;

import java.util.Date;
import java.util.List;

@Component
public class DbEventsReader {
    private static final Log LOG = LogFactory.getLog(DbEventsReader.class);

    @Autowired
    private DbEventsQueue queue;

    @Autowired
    private GetNextEvents getNextEvents;

    @Value("${events.db.redis.maxsize:10000}")
    private long maxSize;

    @Scheduled(cron="${events.getNextEvents:*/60 * * * * ?}")
    public void getNextEvents() {
        logRun();

        try {
            while(readAndPoll()){}
        }
        catch (Exception e) {
            LOG.error("Error running getNextEvents: " + e.getMessage(), e);
        }
    }

    private void logRun() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("run getNextEvents");
        }
    }

    private boolean readAndPoll() {
        if (queue.size() > maxSize) {
            LOG.debug("Skip getNextEvents! Redis queue not empty!");
            return false;
        }
        List<Event> events = getNextEvents.run();
        if (CollectionUtils.isEmpty(events)) {
            return false;
        }

        for (Event event : events) {
           queue.put(convertEvent(event));
        }
        return true;
    }

    private EventRedisDto convertEvent(Event event) {
        return EventRedisDto.builder()
                .date(new Date())
                .eventTypeId(event.getEventTypeId())
                .json(event.getParams())
                .keyValue(event.getKeyValue())
                .organisationId(event.getOrganisationId())
                .build();
    }
}
