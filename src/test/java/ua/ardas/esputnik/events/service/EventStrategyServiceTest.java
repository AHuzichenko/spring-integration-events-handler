package ua.ardas.esputnik.events.service;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import ua.ardas.esputnik.dao.cassandra.errors.CassandraError;
import ua.ardas.esputnik.events.repository.EventsCassandraDao;
import ua.ardas.esputnik.events.repository.entities.EventLog;
import ua.ardas.esputnik.events.domain.EventAction;
import ua.ardas.esputnik.events.domain.StrategyInterval;
import ua.ardas.esputnik.events.queue.EventRedisDto;
import ua.ardas.esputnik.events.repository.EventCampaignByTypeRepository;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByType;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByTypeKey;

@RunWith(DataProviderRunner.class)
public class EventStrategyServiceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private EventsCassandraDao cassandraDao;
	@Mock
	private EventCampaignByTypeRepository eventCampaignByTypeRepository;

	@InjectMocks
	private EventStrategyService testedService;

	@Before
	public void setUp() {
		ReflectionTestUtils
				.setField(testedService, "hHour", Date.from(LocalDate.of(2021, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
	}

	@DataProvider
	public static Object[] strategyAndIntervals() {
		return new Object[][] {
				{"1", StrategyInterval.HOURS.getCode(), true},
				{"1", StrategyInterval.DAYS.getCode(), true},
				{"1", StrategyInterval.WEEKS.getCode(), true},
				{"1", StrategyInterval.MONTHS.getCode(), true},
				{"0", 0, true},
				{"3", StrategyInterval.MONTHS.getCode(), false},
		};
	}

	@Test
	@UseDataProvider("strategyAndIntervals")
	public void checkStrategyIntervalsTest(String strategy, int interval, boolean expected) throws CassandraError, InterruptedException {
		int eventTypeId = 1;
		String keyValue = "keyValue";
		Date date = new Date();

		EventRedisDto event = getEventRedisDto(eventTypeId, keyValue, date);
		EventLog eventLog = getEventLog(eventTypeId, keyValue);
		EventAction eventAction = getEventAction(strategy, interval);

		when(cassandraDao.findLastEventAction(eventTypeId, keyValue)).thenReturn(eventLog);

		boolean actual = testedService.checkStrategy(event, eventAction);

		verify(cassandraDao, only()).findLastEventAction(eventTypeId, keyValue);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void checkEventLogIsAbsentTest() throws CassandraError, InterruptedException {
		int eventTypeId = 1;
		String keyValue = "keyValue";
		Date date = new Date();

		EventRedisDto event = getEventRedisDto(eventTypeId, keyValue, date);
		EventAction eventAction = getEventAction("1", 4);

		when(cassandraDao.findLastEventAction(eventTypeId, keyValue)).thenReturn(null);

		boolean actual = testedService.checkStrategy(event, eventAction);

		verify(cassandraDao, only()).findLastEventAction(eventTypeId, keyValue);
		Assert.assertTrue(actual);
	}

	@Test
	public void checkStrategyIsAbsentTest() throws CassandraError, InterruptedException {
		int eventTypeId = 1;
		String keyValue = "keyValue";
		Date date = new Date();

		EventRedisDto event = getEventRedisDto(eventTypeId, keyValue, date);
		EventAction eventAction = getEventAction("", 0);

		when(cassandraDao.findLastEventAction(eventTypeId, keyValue)).thenReturn(null);

		boolean actual = testedService.checkStrategy(event, eventAction);
		verify(cassandraDao, only()).findLastEventAction(eventTypeId, keyValue);
		Assert.assertTrue(actual);
	}

	@Test
	public void checkStrategyIsAbsentAndEventLogInDBisPresentTest() throws CassandraError, InterruptedException {
		int eventTypeId = 1;
		String keyValue = "keyValue";
		Date date = new Date();

		EventRedisDto event = getEventRedisDto(eventTypeId, keyValue, date);
		EventAction eventAction = getEventAction("", 0);
		EventLog eventLog = getEventLog(eventTypeId, keyValue);

		when(cassandraDao.findLastEventAction(eventTypeId, keyValue)).thenReturn(eventLog);

		boolean actual = testedService.checkStrategy(event, eventAction);
		verify(cassandraDao, only()).findLastEventAction(eventTypeId, keyValue);
		Assert.assertFalse(actual);
	}

	@Test
	public void checkEventActionIsAbsentTest() {
		int eventTypeId = 1;
		String keyValue = "keyValue";
		Date date = new Date();

		EventRedisDto event = getEventRedisDto(eventTypeId, keyValue, date);

		boolean actual = testedService.checkStrategy(event, null);

		verifyNoMoreInteractions(cassandraDao);
		Assert.assertFalse(actual);
	}

	@Test
	public void checkWithLastEventCampaignDateTest() {
		ReflectionTestUtils.setField(testedService, "hHour", Date.from(LocalDate.now().minusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC)));

		int eventTypeId = 1;
		String keyValue = "keyValue";
		Date date = new Date();

		EventRedisDto event = getEventRedisDto(eventTypeId, keyValue, date);
		EventAction eventAction = getEventAction("1", 1); // Month strategy

		EventCampaignByTypeKey key = new EventCampaignByTypeKey(eventTypeId, 1, keyValue);
		when(eventCampaignByTypeRepository.findById(key)).thenReturn(Optional.of(new EventCampaignByType(key, getOlderDate())));

		boolean actual = testedService.checkStrategy(event, eventAction);

		verify(eventCampaignByTypeRepository, only()).findById(key);
		Assert.assertTrue(actual);
	}

	private EventAction getEventAction(String strategy, int interval) {
		EventAction eventAction = EventAction.builder()
			.strategy(strategy)
			.interval(interval)
			.campaignId(1)
			.build();
		return eventAction;
	}

	private EventLog getEventLog(int eventTypeId, String keyValue) {
		return EventLog.builder()
				.eventTypeId(eventTypeId)
				.keyValue(keyValue)
				.date(getOlderDate())
				.build();

	}

	private EventRedisDto getEventRedisDto(int eventTypeId, String keyValue, Date date) {
		return EventRedisDto.builder()
				.eventTypeId(eventTypeId)
				.keyValue(keyValue)
				.date(date)
				.build();
	}

	private Date getOlderDate() {
		return Date.from(LocalDate.now().minusMonths(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}
}