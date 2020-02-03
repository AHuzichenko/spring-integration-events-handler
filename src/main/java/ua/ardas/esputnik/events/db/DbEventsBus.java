package ua.ardas.esputnik.events.db;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ua.ardas.esputnik.commons.dao.dto.event.Event;
import ua.ardas.esputnik.commons.dao.procedures.event.GetNextEvents;
import ua.ardas.esputnik.redis.reliableQueue.bus.MessagesBus;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.MessagesBusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DbEventsBus implements MessagesBus<List<Event>> {
    private static final Log LOG = LogFactory.getLog(DbEventsBus.class);

    @Value("${events.nextEvents.delay:60}")
    private Integer nextEventsDelay;

    @Autowired
    private GetNextEvents getNextEvents;

    @Override
    public List<Event> get() throws MessagesBusException, InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("DbEventsBus. Run getNextEvents");
        }
        List<Event> list = getNextEvents.run();
        if (CollectionUtils.isEmpty(list)) {
            TimeUnit.SECONDS.sleep(nextEventsDelay);
        }
        return list;
    }

    @Override
    public void put(List<Event> events) throws MessagesBusException {

    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public String name() {
        return "events:db";
    }
}
