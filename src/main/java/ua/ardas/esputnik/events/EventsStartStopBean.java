package ua.ardas.esputnik.events;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.events.redis.RedisDbEventsHandler;
import ua.ardas.esputnik.events.redis.RedisFairEventsHandler;
import ua.ardas.esputnik.utils.StartStopBean;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsStartStopBean extends StartStopBean {

	private final RedisDbEventsHandler redisDbEventsHandler;

	@Override
	protected void startImpl() throws InterruptedException {
		log.info("Start event handlers");
		redisDbEventsHandler.doStart();
		log.info("Start event handlers finished");
	}

	@Override
	protected void stopImpl() {
		log.info("Stop event handlers");
		redisDbEventsHandler.doStop();
	}
}
