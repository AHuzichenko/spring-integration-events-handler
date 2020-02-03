package ua.ardas.esputnik.events.redis;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.events.service.EventProcessorService;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFairEventsHandler {

	private final EventProcessorService eventsService;

	@ServiceActivator(inputChannel = "objChannel", async = "true")
	public void process(EventRedisDto event) throws InterruptedException {
		log.info("GOT {}", event);
		if (event.getEventTypeId() == 1) {
			throw new RuntimeException();
		}
//				try {
//					eventsService.processEvents(event);
//				}
//				catch (InterruptedException e) {
//					log.info("handler interrupted");
//					throw e;
//				}
//				catch (RetriableException re) {
//					log.error("Retriable exception while processing event: {}", re.getMessage(), re);
//					throw re;
//				}
//				catch (Exception e) {
//					log.error("Cannot process event: {} ", e.getMessage(), e);
//					throw new RuntimeException(e);
//				}
	}

}
