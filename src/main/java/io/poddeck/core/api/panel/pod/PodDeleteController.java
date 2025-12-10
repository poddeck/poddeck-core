package io.poddeck.core.api.panel.pod;

import io.poddeck.common.PodDeleteRequest;
import io.poddeck.common.PodDeleteResponse;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
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
public final class PodDeleteController extends PodRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private PodDeleteController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @PanelEndpoint
  @RequestMapping(path = "/pod/delete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> deletePod(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var pod = body.getString("pod");
    return findCluster(request)
      .thenCompose(cluster -> deletePod(cluster, namespace, pod));
  }

  private CompletableFuture<Map<String, Object>> deletePod(
    Cluster cluster, String namespace, String pod
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(PodDeleteRequest.newBuilder()
        .setNamespace(namespace).setPod(pod).build(), PodDeleteResponse.class)
      .thenApply(response -> Map.of("success", response.getSuccess()));
  }
}
