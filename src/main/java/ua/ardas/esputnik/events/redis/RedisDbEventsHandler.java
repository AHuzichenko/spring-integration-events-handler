package ua.ardas.esputnik.events.redis;

import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ua.ardas.esputnik.commons.monitoring.metrics.Monitoring;
import ua.ardas.esputnik.events.queue.DbEventsQueue;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.events.service.EventProcessorService;
import ua.ardas.esputnik.redis.reliableQueue.configs.BusReaderConfigsBuilder;
import ua.ardas.esputnik.redis.reliableQueue.consumer.BaseBusReader;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;

@Service
public class RedisDbEventsHandler extends BaseBusReader<EventRedisDto> {
    private static final Log LOG = LogFactory.getLog(RedisDbEventsHandler.class);

    @Value("${events.dbredis.maxThreads:10}")
    private int maxThreads;

    @Autowired
    private DbEventsQueue queue;

    @Autowired
    private EventProcessorService eventsService;

    @PostConstruct
    private void init() {
        setMessagesBus(queue);
        setReaderConfigs(
                new BusReaderConfigsBuilder()
                        .withMaxReaders(maxThreads)
                        .withMaxRetries(30)
                        .withReaderName("events:dbredis:reader")
                        .withAddingMessageToThreadName(false)
                        .build());
        Monitoring.GLOBAL_REGISTRY.gauge("events-handler.db.queue.size", Collections.emptyList(),
                (ToDoubleFunction<List<? extends Object>>) objects -> queue.size());
    }

    protected void process(EventRedisDto event) throws InterruptedException {
        try {
            eventsService.processEvents(event);
        } catch (InterruptedException e) {
            LOG.info("handler interrupted");
            throw e;
        } catch (RetriableException re) {
            LOG.error("Retriable exception while processing event: " + re.getMessage(), re);
            throw re;
        } catch (Exception e) {
            LOG.error("Cannot process event: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getThreadName(EventRedisDto event) {
        return String.format("OrgId: %s; id: %s; key: %s",
                event.getOrganisationId(), event.getEventTypeId(), event.getKeyValue());
    }
}
