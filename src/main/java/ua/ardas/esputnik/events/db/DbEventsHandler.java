package ua.ardas.esputnik.events.db;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ua.ardas.esputnik.commons.dao.dto.event.Event;
import ua.ardas.esputnik.events.service.EventProcessorService;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.redis.reliableQueue.configs.BusReaderConfigsBuilder;
import ua.ardas.esputnik.redis.reliableQueue.consumer.BaseBusReader;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;


@Component
public class DbEventsHandler extends BaseBusReader<List<Event>> {
    private static final Log LOG = LogFactory.getLog(DbEventsHandler.class);

    @Autowired
    private DbEventsBus dbEventsBus;
    @Autowired
    private EventProcessorService eventsService;

    @Value("${events.db.maxThreads:1}")
    private int maxThreads;

    @PostConstruct
    private void init() {
        setMessagesBus(dbEventsBus);
        setReaderConfigs(
            new BusReaderConfigsBuilder()
                .withMaxReaders(maxThreads)
                .withMaxRetries(30)
                .withReaderName("events:db:reader")
                .withAddingMessageToThreadName(false)
                .build());
    }

    @Override
    protected void process(List<Event> events) {
        for (Event event : events) {
            try {
                eventsService.processEvents(convertEvent(event));
            } catch (RetriableException re) {
                throw re;
            } catch (Exception e) {
                LOG.warn("Failed to process event");
                throw new RuntimeException(e);
            }
        }
        LOG.debug("Finished process events: size= " + events.size());
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
