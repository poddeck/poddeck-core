package io.poddeck.core.api.panel.deployment;

import io.poddeck.common.DeploymentCreateRequest;
import io.poddeck.common.DeploymentCreateResponse;
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
public final class DeploymentCreateController extends DeploymentRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private DeploymentCreateController(
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
  @RequestMapping(path = "/deployment/create/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createDeployment(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var raw = body.getString("raw");
    return findCluster(request)
      .thenCompose(cluster -> createDeployment(cluster, raw));
  }

  private CompletableFuture<Map<String, Object>> createDeployment(
    Cluster cluster, String raw
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(DeploymentCreateRequest.newBuilder().setRaw(raw).build(),
        DeploymentCreateResponse.class)
      .thenApply(response -> processCreateDeploymentResult(cluster, response));
  }

  private Map<String, Object> processCreateDeploymentResult(
    Cluster cluster, DeploymentCreateResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.deployment.create.notification.title",
        "panel.deployment.create.notification.description", response.getDeployment());
    }
    return Map.of("success", response.getSuccess(),
      "namespace", response.getNamespace(),
      "deployment", response.getDeployment());
  }
}
