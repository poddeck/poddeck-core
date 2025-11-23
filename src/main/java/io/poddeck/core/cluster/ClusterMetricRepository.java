package io.poddeck.core.cluster;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface ClusterMetricRepository extends DatabaseRepository<ClusterMetric, UUID> {
  @Async
  CompletableFuture<Optional<ClusterMetric>>
  findFirstByClusterAndNodeOrderByTimestampDesc(UUID cluster, String node);
}