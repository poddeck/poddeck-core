package io.poddeck.core.api.panel.workload;

import com.google.common.collect.Maps;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.*;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class WorkloadController extends ClusterRestController {
  private final ClusterMetricRepository metricRepository;

  private WorkloadController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    ClusterMetricRepository metricRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.metricRepository = metricRepository;
  }

  @PanelEndpoint
  @RequestMapping(path = "/workload/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findWorkload(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var node = body.getString("node");
    var start = body.getLong("start");
    var end = body.getLong("end");
    var accuracy = body.getString("accuracy");
    return findCluster(request)
      .thenCompose(cluster -> findWorkload(cluster, node, start, end, accuracy));
  }

  public CompletableFuture<Map<String, Object>> findWorkload(
    Cluster cluster, String node, long start, long end, String accuracy
  ) {
    return metricRepository
      .findAggregatedByClusterAndTimeRange(cluster.id(), node, start, end, accuracy)
      .thenApply(metrics -> metrics.stream()
        .map(this::assembleMetricInformation).toList())
      .thenApply(workload -> Map.of("workload", workload));
  }

  private Map<String, Object> assembleMetricInformation(
    ClusterMetricAggregate metric
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("timestamp", metric.bucket().toEpochMilli());
    information.put("cpu_ratio", metric.cpuRatio());
    information.put("total_memory", metric.totalMemory());
    information.put("used_memory", metric.usedMemory());
    information.put("memory_ratio", metric.memoryRatio());
    information.put("total_storage", metric.totalStorage());
    information.put("used_storage", metric.usedStorage());
    information.put("storage_ratio", metric.storageRatio());
    return information;
  }
}