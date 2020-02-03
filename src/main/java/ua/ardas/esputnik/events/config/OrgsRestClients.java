package ua.ardas.esputnik.events.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.ardas.esputnik.service.organisation.api.client.OrgsServiceClient;
import ua.ardas.esputnik.service.organisation.api.internal.UserApi;

@Configuration
public class OrgsRestClients {

    private final OrgsServiceClient orgsServiceClient;

    public OrgsRestClients(OrgsServiceClient orgsServiceClient) {
        this.orgsServiceClient = orgsServiceClient;
    }

    @Bean
    public UserApi userApiClient() {
        return orgsServiceClient.user();
    }
}
