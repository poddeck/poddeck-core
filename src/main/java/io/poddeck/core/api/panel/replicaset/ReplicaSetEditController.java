package io.poddeck.core.api.panel.replicaset;

import io.poddeck.common.ReplicaSetEditRequest;
import io.poddeck.common.ReplicaSetEditResponse;
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
public final class ReplicaSetEditController extends ReplicaSetRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private ReplicaSetEditController(
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
  @RequestMapping(path = "/replica-set/edit/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> editReplicaSet(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var replicaSet = body.getString("replica_set");
    var raw = body.getString("raw");
    return findCluster(request).thenCompose(cluster ->
      editReplicaSet(cluster, namespace, replicaSet, raw));
  }

  private CompletableFuture<Map<String, Object>> editReplicaSet(
    Cluster cluster, String namespace, String replicaSet, String raw
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(ReplicaSetEditRequest.newBuilder()
          .setNamespace(namespace).setReplicaSet(replicaSet).setRaw(raw).build(),
        ReplicaSetEditResponse.class)
      .thenApply(response -> processEditReplicaSetResult(cluster,
        replicaSet, response));
  }

  private Map<String, Object> processEditReplicaSetResult(
    Cluster cluster, String replicaSet, ReplicaSetEditResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.replica-set.edit.notification.title",
        "panel.replica-set.edit.notification.description", replicaSet);
    }
    return Map.of("success", response.getSuccess());
  }
}
