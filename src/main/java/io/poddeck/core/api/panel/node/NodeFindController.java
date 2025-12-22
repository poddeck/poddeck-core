package io.poddeck.core.api.panel.node;

import io.poddeck.common.NodeFindRequest;
import io.poddeck.common.NodeFindResponse;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterMetricRepository;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.agent.command.AgentCommandFactory;
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
public final class NodeFindController extends NodeRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final ClusterMetricRepository metricRepository;

  private NodeFindController(
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
  @RequestMapping(path = "/node/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findNode(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getString("name");
    return findCluster(request).thenCompose(cluster -> findNode(cluster, name));
  }

  public CompletableFuture<Map<String, Object>> findNode(
    Cluster cluster, String name
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(NodeFindRequest.newBuilder().setName(name).build(),
        NodeFindResponse.class)
      .thenApply(NodeFindResponse::getNode)
      .thenCompose(node ->
        metricRepository.findFirstByClusterAndNodeOrderByTimestampDesc(
          cluster.id(), node.getMetadata().getName())
          .thenApply(metric -> assembleNodeInformation(node, metric.get())))
      .thenApply(node -> Map.of("success", true, "node", node));
  }
}
