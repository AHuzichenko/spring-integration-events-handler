package ua.ardas.esputnik.events.queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import ua.ardas.esputnik.redis.reliableQueue.bus.MessagesBus;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.MessagesBusException;

import javax.annotation.Resource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class DbEventsQueue implements MessagesBus<EventRedisDto> {


    private static final Log LOG = LogFactory.getLog(DbEventsQueue.class);
    private static final long POLL_TIMEOUT_SECOND = 5L;

    @Resource(name = "dbEventQueue")
    private BlockingQueue<EventRedisDto> eventsQueue;


    public EventRedisDto get() throws MessagesBusException, InterruptedException {
        return this.take();
    }

    public void put(EventRedisDto eventRedisDto) throws MessagesBusException {
        this.eventsQueue.offer(eventRedisDto);
    }

    public String name() {
        return "events:dbredis";
    }

    public EventRedisDto take() throws InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("EventsQueue size = %s", Integer.valueOf(this.eventsQueue.size())));
        }
        return this.eventsQueue.poll(POLL_TIMEOUT_SECOND, TimeUnit.SECONDS);
    }

    public long size() {
        return this.eventsQueue.size();
    }

}