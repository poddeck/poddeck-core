package io.poddeck.core.api.panel.daemonset;

import io.poddeck.common.DaemonSetFindRequest;
import io.poddeck.common.DaemonSetFindResponse;
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
public final class DaemonSetFindController extends DaemonSetRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private DaemonSetFindController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @PanelEndpoint
  @RequestMapping(path = "/daemon-set/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findDaemonSet(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var daemonSet = body.getString("daemon_set");
    return findCluster(request)
      .thenCompose(cluster -> findDaemonSet(cluster, namespace, daemonSet));
  }

  private CompletableFuture<Map<String, Object>> findDaemonSet(
    Cluster cluster, String namespace, String daemonSet
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(DaemonSetFindRequest.newBuilder()
          .setNamespace(namespace).setDaemonSet(daemonSet).build(),
        DaemonSetFindResponse.class)
      .thenApply(response -> Map.of("success", response.getSuccess(),
        "daemon_set", assembleDaemonSetInformation(response.getDaemonSet())));
  }
}