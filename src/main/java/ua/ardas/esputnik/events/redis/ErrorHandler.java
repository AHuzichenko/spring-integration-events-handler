package ua.ardas.esputnik.events.redis;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ErrorHandler {

	@ServiceActivator(inputChannel = "errorChannel")
	public void onError(Message<?> message) {
		log.info(message.toString());

	}

}
