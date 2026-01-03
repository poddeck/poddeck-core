package io.poddeck.core.api.panel.daemonset;

import io.poddeck.common.DaemonSetCreateRequest;
import io.poddeck.common.DaemonSetCreateResponse;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.agent.command.AgentCommandFactory;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.notification.NotificationDispatch;
import io.poddeck.core.notification.NotificationType;
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
public final class DaemonSetCreateController extends DaemonSetRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private DaemonSetCreateController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory,
    NotificationDispatch notificationDispatch
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
    this.notificationDispatch = notificationDispatch;
  }

  @PanelEndpoint
  @RequestMapping(path = "/daemon-set/create/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createDaemonSet(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var raw = body.getString("raw");
    return findCluster(request)
      .thenCompose(cluster -> createDaemonSet(cluster, raw));
  }

  private CompletableFuture<Map<String, Object>> createDaemonSet(
    Cluster cluster, String raw
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(DaemonSetCreateRequest.newBuilder().setRaw(raw).build(),
        DaemonSetCreateResponse.class)
      .thenApply(response -> processCreateDaemonSetResult(cluster, response));
  }

  private Map<String, Object> processCreateDaemonSetResult(
    Cluster cluster, DaemonSetCreateResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.daemon-set.create.notification.title",
        "panel.daemon-set.create.notification.description", response.getDaemonSet());
    }
    return Map.of("success", response.getSuccess(),
      "namespace", response.getNamespace(),
      "daemon_set", response.getDaemonSet());
  }
}
