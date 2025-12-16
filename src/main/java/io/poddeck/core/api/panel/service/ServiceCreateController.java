package io.poddeck.core.api.panel.service;

import io.poddeck.common.ServiceCreateRequest;
import io.poddeck.common.ServiceCreateResponse;
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
public final class ServiceCreateController extends ServiceRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private ServiceCreateController(
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
  @RequestMapping(path = "/service/create/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createService(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var raw = body.getString("raw");
    return findCluster(request)
      .thenCompose(cluster -> createService(cluster, raw));
  }

  private CompletableFuture<Map<String, Object>> createService(
    Cluster cluster, String raw
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(ServiceCreateRequest.newBuilder().setRaw(raw).build(),
        ServiceCreateResponse.class)
      .thenApply(response -> processCreateServiceResult(cluster, response));
  }

  private Map<String, Object> processCreateServiceResult(
    Cluster cluster, ServiceCreateResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.service.create.notification.title",
        "panel.service.create.notification.description", response.getService());
    }
    return Map.of("success", response.getSuccess(),
      "namespace", response.getNamespace(),
      "service", response.getService());
  }
}
