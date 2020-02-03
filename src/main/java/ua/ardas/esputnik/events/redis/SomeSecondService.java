package ua.ardas.esputnik.events.redis;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.events.queue.EventRedisDto;

@Slf4j
@Service
public class SomeSecondService {

//	@ServiceActivator(inputChannel = "objChannel", async = "true")
	public void process(EventRedisDto event) {
		log.info("GOT {}", event);
	}
}
