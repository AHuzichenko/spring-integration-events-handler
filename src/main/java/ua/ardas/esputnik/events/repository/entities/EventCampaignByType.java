package ua.ardas.esputnik.events.repository.entities;

import java.util.Date;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Value;

@Value
@Table("event_campaign_by_type")
public class EventCampaignByType {

	@PrimaryKey
	private EventCampaignByTypeKey key;

	@Column("last_date")
	private Date lastDate;

}
