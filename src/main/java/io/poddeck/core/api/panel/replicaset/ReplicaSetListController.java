package io.poddeck.core.api.panel.replicaset;

import io.poddeck.common.ReplicaSetListRequest;
import io.poddeck.common.ReplicaSetListResponse;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
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
public final class ReplicaSetListController extends ReplicaSetRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private ReplicaSetListController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @PanelEndpoint
  @RequestMapping(path = "/replica-sets/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findReplicaSets(
    HttpServletRequest request
  ) {
    return findCluster(request).thenCompose(this::findReplicaSets);
  }

  private CompletableFuture<Map<String, Object>> findReplicaSets(Cluster cluster) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(ReplicaSetListRequest.newBuilder().build(),
        ReplicaSetListResponse.class)
      .thenApply(response -> response.getItemsList().stream()
        .map(this::assembleReplicaSetInformation).toList())
      .thenApply(replicaSets -> Map.of("success", true, "replica_sets", replicaSets));
  }
}