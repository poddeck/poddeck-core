package io.poddeck.core.cluster;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClusterRepository extends DatabaseRepository<Cluster, UUID> {

}