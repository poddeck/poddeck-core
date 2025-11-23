package io.poddeck.core.cluster;

import java.time.Instant;

public record ClusterMetricAggregate(
  Instant bucket,
  Double cpuRatio,
  Double totalMemory,
  Double usedMemory,
  Double memoryRatio,
  Double totalStorage,
  Double usedStorage,
  Double storageRatio
) {}
