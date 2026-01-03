package io.poddeck.core.api.panel.daemonset;

import io.poddeck.common.DaemonSetDeleteRequest;
import io.poddeck.common.DaemonSetDeleteResponse;
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
public final class DaemonSetDeleteController extends DaemonSetRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private DaemonSetDeleteController(
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
  @RequestMapping(path = "/daemon-set/delete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> deleteDaemonSet(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var daemonSet = body.getString("daemon_set");
    return findCluster(request)
      .thenCompose(cluster -> deleteDaemonSet(cluster, namespace, daemonSet));
  }

  private CompletableFuture<Map<String, Object>> deleteDaemonSet(
    Cluster cluster, String namespace, String daemonSet
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(DaemonSetDeleteRequest.newBuilder()
          .setNamespace(namespace).setDaemonSet(daemonSet).build(),
        DaemonSetDeleteResponse.class)
      .thenApply(response -> processDeleteDaemonSetResult(cluster,
        daemonSet, response));
  }

  private Map<String, Object> processDeleteDaemonSetResult(
    Cluster cluster, String daemonSet, DaemonSetDeleteResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.daemon-set.delete.notification.title",
        "panel.daemon-set.delete.notification.description", daemonSet);
    }
    return Map.of("success", response.getSuccess());
  }
}
