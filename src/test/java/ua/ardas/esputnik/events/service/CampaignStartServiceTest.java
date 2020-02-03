package ua.ardas.esputnik.events.service;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import ua.ardas.esputnik.events.cache.CampaignsCache;
import ua.ardas.esputnik.events.cache.EventActionCache;
import ua.ardas.esputnik.events.cache.RunnerUserCache;
import ua.ardas.esputnik.events.domain.Campaign;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.workflows.api.WorkflowsApi;
import ua.ardas.esputnik.workflows.dto.CampaignsRunConditionsDto;
import ua.esputnik.activiti.rest.client.ActivitiRestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CampaignStartServiceTest {
    @InjectMocks
    private CampaignStartService testObj;
    @Mock
    private CampaignsCache campaignsCache;
    @Mock
    private ActivitiRestClient activitiRestClient;
    @Mock
    private CampaignParamsService campaignParamsService;
    @Mock
    private RunnerUserCache runnerUserCache;
    @Mock
    private WorkflowsApi workflowsApi;
    @Mock
    private EventActionCache eventActionCache;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(testObj, "validateUser", true);
    }

	@Test
	public void testStartCampaignShouldChangeRunner() throws ExecutionException {
        int eventTypeId = 3;
        EventAction action = EventAction.builder()
            .userId(1001)
            .organisationId(1)
            .campaignId(5)
            .eventTypeId(eventTypeId)
            .build();

        Campaign campaign = Campaign.builder().build();

        when(campaignsCache.get(5)).thenReturn(campaign);
        when(runnerUserCache.getRunner(action)).thenReturn(1);

        testObj.startCampaign(action, null);

        CampaignsRunConditionsDto run = CampaignsRunConditionsDto.builder()
                .runnerUserId(1)
                .campaignId(5)
                .eventTypeId(eventTypeId)
                .build();

        verify(workflowsApi).putWorkflowRunner(run);
        verify(eventActionCache).clearCacheForSpecificEvent(eventTypeId);
    }


    @Test
    public void testStartCampaignShouldFail() throws ExecutionException {
        EventAction action = EventAction.builder()
                .userId(1001)
                .organisationId(1)
                .campaignId(5)
                .eventTypeId(3)
                .build();

        Campaign campaign = Campaign.builder().build();

        when(campaignsCache.get(5)).thenReturn(campaign);
        when(runnerUserCache.getRunner(action)).thenThrow(new UncheckedExecutionException(new IllegalArgumentException("User with required requiredPermissions wasn't found.")));

        testObj.startCampaign(action, null);
    }
}

