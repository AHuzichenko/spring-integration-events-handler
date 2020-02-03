package ua.ardas.esputnik.events.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import ua.ardas.esputnik.events.repository.entities.EventByType;
import ua.ardas.esputnik.events.repository.entities.EventByTypeKey;

@Repository
public interface EventByTypeRepository extends CassandraRepository<EventByType, EventByTypeKey> {

}