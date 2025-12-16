package io.poddeck.core.api.panel.service;

import io.poddeck.common.ServiceFindRequest;
import io.poddeck.common.ServiceFindResponse;
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
public final class ServiceFindController extends ServiceRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private ServiceFindController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @PanelEndpoint
  @RequestMapping(path = "/service/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findService(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var service = body.getString("service");
    return findCluster(request)
      .thenCompose(cluster -> findService(cluster, namespace, service));
  }

  private CompletableFuture<Map<String, Object>> findService(
    Cluster cluster, String namespace, String service
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(ServiceFindRequest.newBuilder()
          .setNamespace(namespace).setService(service).build(),
        ServiceFindResponse.class)
      .thenApply(response -> Map.of("success", response.getSuccess(),
        "service", assembleServiceInformation(response.getService())));
  }
}