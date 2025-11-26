package io.poddeck.core.cluster;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface ClusterMetricRepository extends DatabaseRepository<ClusterMetric, UUID> {
  @Async
  CompletableFuture<Optional<ClusterMetric>>
  findFirstByClusterAndNodeOrderByTimestampDesc(UUID cluster, String node);

  @Query(value = """
    SELECT
      date_trunc(:accuracy, to_timestamp(timestamp / 1000)) as bucket,
      avg(cpu_ratio) as cpu_ratio,
      avg(total_memory) as total_memory,
      avg(used_memory) as used_memory,
      avg(memory_ratio) as memory_ratio,
      avg(total_storage) as total_storage,
      avg(used_storage) as used_storage,
      avg(storage_ratio) as storage_ratio
    FROM cluster_metric
    WHERE cluster = :cluster
      AND node = :node
      AND timestamp BETWEEN :start AND :end
    GROUP BY bucket
    ORDER BY bucket
    """, nativeQuery = true
  )
  @Async
  CompletableFuture<List<ClusterMetricAggregate>> findAggregatedByClusterAndTimeRange(
    @Param("cluster") UUID cluster,
    @Param("node") String node,
    @Param("start") long start,
    @Param("end") long end,
    @Param("accuracy") String accuracy
  );

  @Async
  CompletableFuture<Void> deleteByCluster(UUID cluster);
}