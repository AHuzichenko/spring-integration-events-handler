package ua.ardas.esputnik.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.IntegrationComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "ua.ardas.esputnik.events",
        "ua.ardas.esputnik.service.organisation.api.client",
        "ua.ardas.esputnik.services.clients.organisation",
        "ua.ardas.esputnik.workflows.client",
        "ua.ardas.esputnik.redis.reliableQueue"
},
        basePackageClasses = {
            EventsWebSecurityConfig.class
        })
@ImportResource({"classpath:eventsHandlerContext.xml"})
public class EventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsApplication.class, args);
    }
}
