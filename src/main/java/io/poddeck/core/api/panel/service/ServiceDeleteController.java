package io.poddeck.core.api.panel.service;

import io.poddeck.common.ServiceDeleteRequest;
import io.poddeck.common.ServiceDeleteResponse;
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
public final class ServiceDeleteController extends ServiceRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private ServiceDeleteController(
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
  @RequestMapping(path = "/service/delete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> deleteService(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var service = body.getString("service");
    return findCluster(request)
      .thenCompose(cluster -> deleteService(cluster, namespace, service));
  }

  private CompletableFuture<Map<String, Object>> deleteService(
    Cluster cluster, String namespace, String service
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(ServiceDeleteRequest.newBuilder()
          .setNamespace(namespace).setService(service).build(),
        ServiceDeleteResponse.class)
      .thenApply(response -> processDeleteServiceResult(cluster,
        service, response));
  }

  private Map<String, Object> processDeleteServiceResult(
    Cluster cluster, String service, ServiceDeleteResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.service.delete.notification.title",
        "panel.service.delete.notification.description", service);
    }
    return Map.of("success", response.getSuccess());
  }
}
