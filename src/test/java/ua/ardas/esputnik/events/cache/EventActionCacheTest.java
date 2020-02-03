package ua.ardas.esputnik.events.cache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.com.google.common.collect.Lists;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.events.repository.EventActionRepository;
import ua.ardas.esputnik.redis.reliableQueue.exceptions.RetriableException;

import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EventActionCacheTest {

    @InjectMocks
    private EventActionCache testObj;
    @Mock
    private EventActionRepository eventActionSelect;

    @Before
    public void setUp() {
        ReflectionTestUtils.invokeMethod(testObj, "postConstruct");

    }

    @Test
    public void should_return_active_event_action() {

        EventAction expectedAction = EventAction.builder()
                .eventTypeId(1)
                .campaignActive(true)
                .campaignPaused(false)
                .build();

        when(eventActionSelect.find(1)).thenReturn(Collections.singletonList(expectedAction));

        EventAction actualAction = testObj.getByEventTypeId(1);

        Assert.assertEquals(expectedAction, actualAction);

        verify(eventActionSelect, only()).find(1);
    }

    @Test
    public void should_return_null_if_campaign_not_active() {

        EventAction notActiveAction = EventAction.builder()
                .eventTypeId(1)
                .campaignActive(false)
                .campaignPaused(false)
                .build();

        when(eventActionSelect.find(1)).thenReturn(Collections.singletonList(notActiveAction));

        EventAction actualAction = testObj.getByEventTypeId(1);

        Assert.assertNull(actualAction);

        verify(eventActionSelect, only()).find(1);
    }

    @Test
    public void should_return_null_if_campaign_paused() {

        EventAction pausedAction = EventAction.builder()
                .eventTypeId(1)
                .campaignActive(true)
                .campaignPaused(true)
                .build();

        when(eventActionSelect.find(1)).thenReturn(Collections.singletonList(pausedAction));

        EventAction actualAction = testObj.getByEventTypeId(1);

        Assert.assertNull(actualAction);

        verify(eventActionSelect, only()).find(1);
    }


    @Test
    public void should_return_null_if_event_action_is_not_exist() {

        when(eventActionSelect.find(1)).thenReturn(Lists.newArrayList());

        Assert.assertNull(testObj.getByEventTypeId(1));
    }

    @Test(expected = RetriableException.class)
    public void should_throw_retry_if_db_error() {

        when(eventActionSelect.find(1)).thenThrow(new DataAccessException("DB Fail") {
        });

        Assert.assertNull(testObj.getByEventTypeId(1));
    }

}