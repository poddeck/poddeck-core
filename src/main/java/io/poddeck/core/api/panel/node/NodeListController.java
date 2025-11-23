package io.poddeck.core.api.panel.node;

import com.google.common.collect.Maps;
import io.poddeck.common.Node;
import io.poddeck.common.NodeListRequest;
import io.poddeck.common.NodeListResponse;
import io.poddeck.common.iterator.AsyncIterator;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterMetric;
import io.poddeck.core.cluster.ClusterMetricRepository;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.agent.command.AgentCommandFactory;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class NodeListController extends ClusterRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final ClusterMetricRepository metricRepository;

  private NodeListController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory,
    ClusterMetricRepository metricRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
    this.metricRepository = metricRepository;
  }

  @RequestMapping(path = "/nodes/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findNodes(
    HttpServletRequest request
  ) {
    return findCluster(request).thenCompose(this::findNodes);
  }

  public CompletableFuture<Map<String, Object>> findNodes(Cluster cluster) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(NodeListRequest.newBuilder().build(), NodeListResponse.class)
      .thenCompose(nodeListResponse -> AsyncIterator.execute(
        nodeListResponse.getItemsList(), node ->
          metricRepository.findFirstByClusterAndNodeOrderByTimestampDesc(
            cluster.id(), node.getMetadata().getName()).thenApply(metric ->
            assembleNodeInformation(node, metric.get()))))
      .thenApply(nodes -> Map.of("nodes", nodes));
  }

  private Map<String, Object> assembleNodeInformation(
    Node node, ClusterMetric metric
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", node.getMetadata().getName());
    information.put("cpu_cores", metric.cpuCores());
    information.put("cpu_ratio", metric.cpuRatio());
    information.put("total_memory", metric.totalMemory());
    information.put("used_memory", metric.usedMemory());
    information.put("memory_ratio", metric.memoryRatio());
    information.put("total_storage", metric.totalStorage());
    information.put("used_storage", metric.usedStorage());
    information.put("storage_ratio", metric.storageRatio());
    information.put("version", node.getStatus().getInfo().getKubeletVersion());
    information.put("ready", node.getStatus().getCondition().getIsReady());
    return information;
  }
}
