package io.poddeck.core.api.panel.pod;

import io.poddeck.common.PodDeleteRequest;
import io.poddeck.common.PodDeleteResponse;
import io.poddeck.common.PodLogRequest;
import io.poddeck.common.PodLogResponse;
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
public final class PodLogController extends ClusterRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private PodLogController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @RequestMapping(path = "/pod/log/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPodLog(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var pod = body.getString("pod");
    var sinceSeconds = body.has("since_seconds") ?
      body.getInt("since_seconds") : -1;
    return findCluster(request)
      .thenCompose(cluster -> deletePod(cluster, namespace, pod, sinceSeconds));
  }

  public CompletableFuture<Map<String, Object>> deletePod(
    Cluster cluster, String namespace, String pod, int sinceSeconds
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    var request = PodLogRequest.newBuilder()
      .setNamespace(namespace).setPod(pod).setSinceSeconds(sinceSeconds).build();
    return commandFactory.create(agent.get())
      .execute(request, PodLogResponse.class)
      .thenApply(response -> Map.of("success", true, "logs", response.getLogs()));
  }
}
