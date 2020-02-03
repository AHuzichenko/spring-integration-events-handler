package ua.ardas.esputnik.events.repository.entities;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.NonNull;
import lombok.Value;

@Value
@PrimaryKeyClass
public class EventCampaignByTypeKey {

	@PrimaryKeyColumn(name = "event_type_id", type = PrimaryKeyType.PARTITIONED)
	@NonNull
	private Integer typeId;

	@PrimaryKeyColumn(name = "campaign_id")
	@NonNull
	private Integer campaignId;

	@PrimaryKeyColumn(value = "key_value")
	@NonNull
	private String keyValue;
}