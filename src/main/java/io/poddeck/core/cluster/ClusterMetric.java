package io.poddeck.core.cluster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Entity
@Table(name = "cluster_metric")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class ClusterMetric {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "cluster", nullable = false)
  private UUID cluster;
  @Column(name = "node", nullable = false)
  private String node;
  @Column(name = "cpu_cores", nullable = false)
  private int cpuCores;
  @Column(name = "cpu_ratio", nullable = false)
  private double cpuRatio;
  @Column(name = "total_memory", nullable = false)
  private double totalMemory;
  @Column(name = "used_memory", nullable = false)
  private double usedMemory;
  @Column(name = "memory_ratio", nullable = false)
  private double memoryRatio;
  @Column(name = "total_storage", nullable = false)
  private double totalStorage;
  @Column(name = "used_storage", nullable = false)
  private double usedStorage;
  @Column(name = "storage_ratio", nullable = false)
  private double storageRatio;
  @Column(name = "timestamp", nullable = false)
  private long timestamp;
}
