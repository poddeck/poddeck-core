package io.poddeck.core.api.panel.node;

import io.poddeck.common.NodeListRequest;
import io.poddeck.common.NodeListResponse;
import io.poddeck.common.iterator.AsyncIterator;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
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
public final class NodeListController extends NodeRestController {
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

  @PanelEndpoint
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
      .thenApply(nodes -> Map.of("success", true, "nodes", nodes));
  }
}
