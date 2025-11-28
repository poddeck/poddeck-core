package io.poddeck.core.api.panel.namespace;

import io.poddeck.common.NamespaceDeleteRequest;
import io.poddeck.common.NamespaceDeleteResponse;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.cluster.Cluster;
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
public final class NamespaceDeleteController extends ClusterRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private NamespaceDeleteController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @RequestMapping(path = "/namespace/delete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> deleteNamespace(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getString("name");
    return findCluster(request)
      .thenCompose(cluster -> deleteNamespace(cluster, name));
  }

  public CompletableFuture<Map<String, Object>> deleteNamespace(
    Cluster cluster, String name
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(NamespaceDeleteRequest.newBuilder().setName(name).build(),
        NamespaceDeleteResponse.class)
      .thenApply(response -> Map.of("success", response.getSuccess()));
  }
}
