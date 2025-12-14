package io.poddeck.core.api.panel.namespace;

import io.poddeck.common.NamespaceCreateRequest;
import io.poddeck.common.NamespaceCreateResponse;
import io.poddeck.core.api.panel.ClusterRestController;
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
public final class NamespaceCreateController extends ClusterRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private NamespaceCreateController(
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
  @RequestMapping(path = "/namespace/create/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createNamespace(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getString("name");
    return findCluster(request)
      .thenCompose(cluster -> createNamespace(cluster, name));
  }

  public CompletableFuture<Map<String, Object>> createNamespace(
    Cluster cluster, String name
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(NamespaceCreateRequest.newBuilder().setName(name).build(),
        NamespaceCreateResponse.class)
      .thenApply(response -> processCreateNamespaceResult(cluster,
        name, response));
  }

  private Map<String, Object> processCreateNamespaceResult(
    Cluster cluster, String namespace, NamespaceCreateResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.namespace.create.notification.title",
        "panel.namespace.create.notification.description", namespace);
    }
    return Map.of("success", response.getSuccess());
  }
}
