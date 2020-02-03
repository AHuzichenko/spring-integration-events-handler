package ua.ardas.esputnik.events.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;

import ua.ardas.esputnik.events.repository.entities.EventCampaignByType;
import ua.ardas.esputnik.events.repository.entities.EventCampaignByTypeKey;

public interface EventCampaignByTypeRepository extends CassandraRepository<EventCampaignByType, EventCampaignByTypeKey> {
}
