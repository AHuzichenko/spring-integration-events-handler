package ua.ardas.esputnik.events.repository.mappers;

import java.util.UUID;

import com.datastax.driver.core.Row;

import ua.ardas.esputnik.dao.cassandra.base.RowMapper;
import ua.ardas.esputnik.events.repository.entities.EventLog;

public class EventLogRowMapper implements RowMapper<EventLog> {

	@Override
	public EventLog mapRow(Row row) {
		return EventLog.builder()
				.actions(row.getString("actions"))
				.date(row.getTimestamp("date"))
				.eventId(toString(row.getUUID("event_id")))
				.eventTypeId(row.getInt("event_type_id"))
				.keyValue(row.getString("key_value"))
				.organisationId(row.getInt("organisation_id"))
				.params(row.getString("params"))
				.build();

	}

	private String toString(UUID uuid) {
		return null != uuid ? uuid.toString() : null;
	}

}
