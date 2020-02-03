package ua.ardas.esputnik.events.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import ua.ardas.esputnik.dao.cassandra.base.CassandraDao;
import ua.ardas.esputnik.dao.cassandra.errors.CassandraError;
import ua.ardas.esputnik.events.repository.entities.EventLog;
import ua.ardas.esputnik.stat.StatsdMethod;

@Repository
public class EventsCassandraDao extends CassandraDao {

	private static final String EVENTS = "events";

	@Value("${cassandra.hosts:events-cassandra-host,cassandra-host2,cassandra-host3}")
	private String cassandraHosts;

	@StatsdMethod
	public EventLog findLastEventAction(final int eventTypeId, final String keyValue) throws InterruptedException, CassandraError {
		return findOne("SELECT date FROM event_actions WHERE event_type_id=? AND key_value=? LIMIT 1", row -> EventLog.builder()
				.eventTypeId(eventTypeId)
				.keyValue(keyValue)
				.date(row.getTimestamp("date"))
				.build(), eventTypeId, keyValue);
	}
	
	@Override
	protected String getContactHosts() {
		return cassandraHosts;
	}
	
	@Override
	protected String getKeySpace() {
		return EVENTS;
	}
}
