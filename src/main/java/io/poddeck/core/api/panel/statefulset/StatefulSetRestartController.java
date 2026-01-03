package io.poddeck.core.api.panel.statefulset;

import io.poddeck.common.StatefulSetRestartRequest;
import io.poddeck.common.StatefulSetRestartResponse;
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
public final class StatefulSetRestartController extends StatefulSetRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private StatefulSetRestartController(
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
  @RequestMapping(path = "/stateful-set/restart/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> restartStatefulSet(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var statefulSet = body.getString("stateful_set");
    return findCluster(request)
      .thenCompose(cluster -> restartStatefulSet(cluster, namespace, statefulSet));
  }

  private CompletableFuture<Map<String, Object>> restartStatefulSet(
    Cluster cluster, String namespace, String statefulSet
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(StatefulSetRestartRequest.newBuilder()
          .setNamespace(namespace).setStatefulSet(statefulSet).build(),
        StatefulSetRestartResponse.class)
      .thenApply(response -> processRestartStatefulSetResult(cluster,
        statefulSet, response));
  }

  private Map<String, Object> processRestartStatefulSetResult(
    Cluster cluster, String statefulSet, StatefulSetRestartResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.stateful-set.restart.notification.title",
        "panel.stateful-set.restart.notification.description", statefulSet);
    }
    return Map.of("success", response.getSuccess());
  }
}
