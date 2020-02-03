package ua.ardas.esputnik.events.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.common.base.Predicate;

import lombok.extern.slf4j.Slf4j;
import ua.ardas.esputnik.dao.cassandra.base.CassandraDao;
import ua.ardas.esputnik.dao.cassandra.errors.CassandraError;
import ua.ardas.esputnik.events.repository.entities.EventLog;
import ua.ardas.esputnik.events.repository.mappers.EventLogRowMapper;
import ua.ardas.esputnik.stat.StatsdMethod;
import ua.ardas.esputnik.utils.timebuckets.TimeBuckets;

@Slf4j
@Repository
public class EventBucketsCassandraDao extends CassandraDao {

	private static final String EVENTS = "events";

    public static final String SELECT_BUCKET =
        "SELECT organisation_id, date, event_id, event_type_id, actions, key_value, params " +
        "FROM event_buckets WHERE time_bucket = ? AND organisation_id = ?";
    public static final String EVENT_TYPE_FILTER = " AND event_type_id = ";

    protected static final String EVENT_BUCKET_BY_TYPE = "event_buckets_by_type";
    protected static final String EVENT_BUCKET_BY_DIVISION = "event_buckets_by_division";
    protected static final String INSERT_EVENT = "INSERT INTO %s (" +
            "	time_bucket,event_id,organisation_id,event_type_id,key_value,date,actions,params" +
            " ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Value("${cassandra.hosts:events-cassandra-host,cassandra-host2,cassandra-host3}")
	private String cassandraHosts;

    @StatsdMethod
    @Deprecated
    public List<EventLog> searchEventBuckets(int orgId, Integer typeId, String keyValueSearch,
											 final Date dateFrom, final Date dateTo, int limit) throws InterruptedException {
        List<EventLog> res = new ArrayList<>();
        Date date = dateTo;
		DateRangeFilter dateRangePredicate = new DateRangeFilter(dateFrom, dateTo);
        while (dateFrom.before(date) && res.size() <= limit) {
            int remain = limit - res.size();
			List<EventLog> eventBucketEvents = getEventBucketEvents(orgId, typeId, keyValueSearch, getTimeBucket(date), remain);
			List<EventLog> eventsWithinDateRange = eventBucketEvents.stream().filter(dateRangePredicate::apply).collect(Collectors.toList());

			res.addAll(eventsWithinDateRange);
            date = DateUtils.addDays(date, -1);
        }
        return res;
    }

    @StatsdMethod
    public void storeEventByType(EventLog eventLog) throws InterruptedException, CassandraError {
        storeEvent(eventLog, EVENT_BUCKET_BY_TYPE);
    }

    @StatsdMethod
    public void storeEventByDivision(EventLog eventLog) throws InterruptedException, CassandraError {
        storeEvent(eventLog, EVENT_BUCKET_BY_DIVISION);
    }

    private void storeEvent(EventLog eventLog, String table) throws InterruptedException, CassandraError {
        executePreparedStatement(String.format(INSERT_EVENT, table),
                getTimeBucket(eventLog.getDate()),
                toUUID(eventLog.getEventId()),
                eventLog.getOrganisationId(),
                eventLog.getEventTypeId(),
                eventLog.getKeyValue(),
                eventLog.getDate(),
                eventLog.getActions(),
                eventLog.getParams());
    }

    private List<EventLog> getEventBucketEvents(int orgId, Integer typeId, String keyValueSearch, long bucket, int limit)
            throws InterruptedException {
        String cql = null == typeId ? SELECT_BUCKET
                : SELECT_BUCKET + EVENT_TYPE_FILTER + typeId;
        return findAllNonBlocking(cql, new EventLogRowMapper(), makeKeyValuePredicate(keyValueSearch),
                limit, bucket, orgId);
    }

    protected long getTimeBucket(Date date) {
        return TimeBuckets.DAY.currentTimeBucket(date).getTime();
    }

	private Predicate<EventLog> makeKeyValuePredicate(final String keyValueSearch) {
		return null == keyValueSearch ? null : new KeyValuePredicate(keyValueSearch);
	}

	@Override
	protected String getContactHosts() {
		return cassandraHosts;
	}
	
	@Override
	protected String getKeySpace() {
		return EVENTS;
	}
	
	private static class KeyValuePredicate implements Predicate<EventLog> {

		private final String searchStr;
		
		public KeyValuePredicate(String searchStr) {
			this.searchStr = searchStr;
		}

		@Override
		public boolean apply(EventLog input) {
			return StringUtils.containsIgnoreCase(input.getKeyValue(), searchStr);
		}
		
	}

	private class DateRangeFilter implements Predicate<EventLog> {
    	private Date dateFrom;
    	private Date dateTo;

		public DateRangeFilter(Date dateFrom, Date dateTo) {
			this.dateFrom = dateFrom;
			this.dateTo = dateTo;
		}

		@Override
		public boolean apply(EventLog event) {
			return dateFrom.before(event.getDate()) && event.getDate().before(dateTo);
		}
	}
}
