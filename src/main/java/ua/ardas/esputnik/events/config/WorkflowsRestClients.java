package ua.ardas.esputnik.events.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.ardas.esputnik.workflows.api.WorkflowsApi;
import ua.ardas.esputnik.workflows.client.WorkflowsServiceClient;

@Configuration
public class WorkflowsRestClients {

    private final WorkflowsServiceClient workflowsServiceClient;

    public WorkflowsRestClients(WorkflowsServiceClient workflowsServiceClient) {
        this.workflowsServiceClient = workflowsServiceClient;
    }

    @Bean
    public WorkflowsApi workflowsApiClient() {
        return workflowsServiceClient.createWorkflowsApiClient();
    }
}
