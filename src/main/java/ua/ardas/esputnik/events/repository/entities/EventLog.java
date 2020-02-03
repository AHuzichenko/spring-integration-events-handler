package ua.ardas.esputnik.events.repository.entities;

import java.util.Date;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EventLog {

	private int eventTypeId;
	private int organisationId;
	private String keyValue;
	private String actions;
	private String params;
	
	private Date date;
	private String eventId;
}
