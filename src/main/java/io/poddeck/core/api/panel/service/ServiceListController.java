package io.poddeck.core.api.panel.service;

import io.poddeck.common.ServiceListRequest;
import io.poddeck.common.ServiceListResponse;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.agent.command.AgentCommandFactory;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ServiceListController extends ServiceRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private ServiceListController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @PanelEndpoint
  @RequestMapping(path = "/services/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findServices(
    HttpServletRequest request
  ) {
    return findCluster(request).thenCompose(this::findServices);
  }

  private CompletableFuture<Map<String, Object>> findServices(Cluster cluster) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(ServiceListRequest.newBuilder().build(),
        ServiceListResponse.class)
      .thenApply(response -> response.getItemsList().stream()
        .map(this::assembleServiceInformation).toList())
      .thenApply(services -> Map.of("success", true, "services", services));
  }
}