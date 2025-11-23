package io.poddeck.core.communication.metric;

import io.poddeck.common.Metric;
import io.poddeck.common.MetricReport;
import io.poddeck.core.cluster.ClusterMetric;
import io.poddeck.core.cluster.ClusterMetricRepository;
import io.poddeck.core.communication.agent.Agent;
import io.poddeck.core.communication.service.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class MetricService implements Service<MetricReport> {
  private final ClusterMetricRepository metricRepository;

  public void process(
    Agent agent, MetricReport metricReport
  ) {
    for (var entry : metricReport.getMetricsList()) {
      metricRepository.generateAvailableId(UUID::randomUUID)
        .thenAccept(id -> storeMetric(agent, entry, id));
    }
  }

  private void storeMetric(Agent agent, Metric entry, UUID id) {
    var metric = ClusterMetric.create(id, agent.cluster(), entry.getNode(),
      entry.getCpuCores(), entry.getCpuRatio(), entry.getTotalMemory(),
      entry.getUsedMemory(), entry.getMemoryRatio(),
      entry.getTotalStorage(), entry.getUsedStorage(),
      entry.getStorageRatio(), System.currentTimeMillis());
    metricRepository.save(metric);
  }
}