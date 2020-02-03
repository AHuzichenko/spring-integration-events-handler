package ua.ardas.esputnik.events.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import ua.ardas.esputnik.events.queue.EventRedisDto;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Service
public class CopyEventService {
    private static final Gson GSON = new Gson();

    private static final String EVENTS_COPY_QUEUE = "events:copy:queue";

    @Value("${events.copy.feature.enabled:false}")
    private boolean copyEventFeature;

    @Autowired
    @Qualifier("transactionsRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    private DataProcessor eventDataCopyProcesser = data -> {//NO-op implementation
    };

    @PostConstruct
    public void initProcessor() {
        log.warn("Prepare init copy processor");
        try {
            if (copyEventFeature) {
                log.warn("Start init copy processor");
                Flux.<EventRedisDto>push(
                    fluxSink -> eventDataCopyProcesser = t -> {
                        try {
                            fluxSink.next(t);
                        } catch (Exception e) {
                            log.error("Can not emmit next event to copy", e);
                        }
                    })
                    .bufferTimeout(100, Duration.ofSeconds(15))
                    .filter(eventRedisDtos -> {
                        Long size = redisTemplate.opsForList().size(EVENTS_COPY_QUEUE);
                        boolean emptyQueue = null == size || size < 10_000;
                        if (!emptyQueue) {
                            log.warn("Skip copy due to full copy event queue!");
                        }
                        return emptyQueue;
                    })
                    .doOnNext(eventRedisDtos -> log.debug("Copying events {}", CollectionUtils.size(eventRedisDtos)))
                    .doOnNext(event -> redisTemplate.opsForList().leftPush(EVENTS_COPY_QUEUE, GSON.toJson(event)))
                    .doOnError(throwable -> log.error("Can not copy event!", throwable))
                    .subscribeOn(Schedulers.newSingle("copy-event-processor"))
                    .subscribe();
            }
        } catch (Exception e) {
            log.error("Can not init copy event processor");
        }
    }

    public void copyEvent(EventRedisDto event) {
        eventDataCopyProcesser.onReceive(event);
    }

    private interface DataProcessor {
        void onReceive(EventRedisDto data);
    }
}
