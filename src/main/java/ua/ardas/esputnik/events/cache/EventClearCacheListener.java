package ua.ardas.esputnik.events.cache;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class EventClearCacheListener {

    private static final Log LOG = LogFactory.getLog(EventClearCacheListener.class);
    private Gson gson = new Gson();

    @Autowired
    private EventActionCache eventActionCache;

    @Autowired
    @Qualifier("messageListenerContainer")
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @PostConstruct
    private void configureListener() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(this);
        adapter.afterPropertiesSet();
        redisMessageListenerContainer.addMessageListener(adapter,new ChannelTopic("clearCache:event"));
    }

    public void handleMessage(String message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Received clear cache request for event: %s", message));
        }

        eventActionCache.clearCacheForSpecificEvent(gson.fromJson(message, Integer.class));

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Event handler cache has been cleared: %s", message));
        }
    }
}
