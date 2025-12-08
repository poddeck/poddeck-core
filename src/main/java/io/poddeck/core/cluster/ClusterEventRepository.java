package io.poddeck.core.cluster;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface ClusterEventRepository extends DatabaseRepository<ClusterEvent, UUID> {
  @Async
  @Query("""
    SELECT c FROM ClusterEvent c
    WHERE c.cluster = :cluster
      AND c.lastTimestamp BETWEEN :start AND :end
    ORDER BY c.lastTimestamp DESC
    """)
  CompletableFuture<List<ClusterEvent>> findEventsByClusterAndTimeRange(
    @Param("cluster") UUID cluster,
    @Param("start") long start,
    @Param("end") long end,
    Pageable pageable
  );

  @Async
  CompletableFuture<Void> deleteByCluster(UUID cluster);
}