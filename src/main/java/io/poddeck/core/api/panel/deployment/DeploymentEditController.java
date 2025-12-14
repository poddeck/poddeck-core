package io.poddeck.core.api.panel.deployment;

import io.poddeck.common.DeploymentEditRequest;
import io.poddeck.common.DeploymentEditResponse;
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
public final class DeploymentEditController extends DeploymentRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private DeploymentEditController(
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
  @RequestMapping(path = "/deployment/edit/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> editDeployment(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var deployment = body.getString("deployment");
    var raw = body.getString("raw");
    return findCluster(request).thenCompose(cluster ->
      editDeployment(cluster, namespace, deployment, raw));
  }

  private CompletableFuture<Map<String, Object>> editDeployment(
    Cluster cluster, String namespace, String deployment, String raw
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(DeploymentEditRequest.newBuilder()
          .setNamespace(namespace).setDeployment(deployment).setRaw(raw).build(),
        DeploymentEditResponse.class)
      .thenApply(response -> processEditDeploymentResult(cluster,
        deployment, response));
  }

  private Map<String, Object> processEditDeploymentResult(
    Cluster cluster, String deployment, DeploymentEditResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.deployment.edit.notification.title",
        "panel.deployment.edit.notification.description", deployment);
    }
    return Map.of("success", response.getSuccess());
  }
}
