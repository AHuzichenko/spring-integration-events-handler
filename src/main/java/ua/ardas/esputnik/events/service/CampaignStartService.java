package ua.ardas.esputnik.events.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.UncheckedExecutionException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.commons.monitoring.metrics.Monitoring;
import ua.ardas.esputnik.dto.ParamWrapper;
import ua.ardas.esputnik.events.cache.CampaignsCache;
import ua.ardas.esputnik.events.cache.EventActionCache;
import ua.ardas.esputnik.events.cache.EventTypeKeyCache;
import ua.ardas.esputnik.events.cache.RunnerUserCache;
import ua.ardas.esputnik.events.domain.Campaign;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.workflows.api.WorkflowsApi;
import ua.ardas.esputnik.workflows.dto.CampaignsRunConditionsDto;
import ua.esputnik.activiti.rest.client.ActivitiRestClient;
import ua.esputnik.activiti.rest.client.dto.CampaignDto;
import ua.esputnik.activiti.rest.client.exception.ActivitiRestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignStartService {

    private static Counter actionsCounter = Monitoring.GLOBAL_REGISTRY.counter("events-handler.actions.processed");
    private static Timer actionsTimer = Monitoring.GLOBAL_REGISTRY.timer("events-handler.actions.time");

    private final CampaignsCache campaignsCache;
    private final ActivitiRestClient activitiRestClient;
    private final CampaignParamsService campaignParamsService;
    private final RunnerUserCache runnerUserCache;
    private final WorkflowsApi workflowsApi;
    private final EventActionCache eventActionCache;


    @Value("${events.warn.duration.sec:3}")
    private int warnDuration;
    @Value("${events.validate.required.params:true}")
    private boolean validateRequiredParams;
    @Value("${events.validate.user:true}")
    private boolean validateUser;

    public void startCampaign(EventAction eventAction, EventRedisDto event) {
        long start = System.currentTimeMillis();
        int campaignId = eventAction.getCampaignId();
        int organisationId = eventAction.getOrganisationId();
        Campaign campaign = campaignsCache.get(campaignId);

        if (null == campaign) {
            log.warn("Cannot find campaign Id: {} for organisationId: {}", campaignId, organisationId);
            return;
        }

        if (campaign.isInvalidJsonFigures() && validateRequiredParams) {
            log.warn("Campaign with id {} is not complete! And Should be skipped", campaign.getCampaignId());
            return;
        }
        try {
            int userId = getRunner(eventAction);

            CampaignDto dto = new CampaignDto(campaignId, campaign.getSchemaId(), campaign.getParams(), campaign.getMetadata());
            dto.setUserId(userId);
            dto.setOrganizationId(organisationId);

            ParamWrapper campaignParams = campaignParamsService.transformParams(event);

            log.trace("Starting campaign {} with user {} and params: {}", campaign.getCampaignId(), userId, campaignParams);
            log.debug("Starting campaign {} with user {}", campaign.getCampaignId(), userId);

            activitiRestClient.startCampaign(dto, campaignParams);
        }
        catch (ActivitiRestClientException | ExecutionException | UncheckedExecutionException e) {
            log.error("Cannot start campaign {}, err = {}", campaignId, e.getMessage(), e);
        }

        long elapsed = System.currentTimeMillis() - start;
        actionsCounter.increment();
        actionsTimer.record(elapsed, TimeUnit.MILLISECONDS);

        if (elapsed > TimeUnit.SECONDS.toMillis(warnDuration)) {
            log.warn("Start campaign long. id: {}, elapsed: {}", campaignId, elapsed);
        }
    }

    private int getRunner(EventAction eventAction) throws ExecutionException {
        if (!validateUser) {
            return eventAction.getUserId();
        }

        int userId = runnerUserCache.getRunner(eventAction);

        if (null == eventAction.getRunnerUserId() || !eventAction.getRunnerUserId().equals(userId)) {
            log.info("store campaign runner: campaign={}, runner: {}", eventAction.getCampaignId(), userId);
            CampaignsRunConditionsDto runConditions = CampaignsRunConditionsDto.builder()
                    .campaignId(eventAction.getCampaignId())
                    .eventTypeId(eventAction.getEventTypeId())
                    .runnerUserId(userId)
                    .build();
            workflowsApi.putWorkflowRunner(runConditions);
            eventActionCache.clearCacheForSpecificEvent(eventAction.getEventTypeId());
        }

        return userId;
    }
}
