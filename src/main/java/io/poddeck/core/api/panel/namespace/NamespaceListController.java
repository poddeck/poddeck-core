package io.poddeck.core.api.panel.namespace;

import com.google.common.collect.Maps;
import io.poddeck.common.Namespace;
import io.poddeck.common.NamespaceListRequest;
import io.poddeck.common.NamespaceListResponse;
import io.poddeck.core.api.panel.ClusterRestController;
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
public final class NamespaceListController extends ClusterRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private NamespaceListController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @PanelEndpoint
  @RequestMapping(path = "/namespaces/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findNamespaces(
    HttpServletRequest request
  ) {
    return findCluster(request).thenCompose(this::findNamespaces);
  }

  public CompletableFuture<Map<String, Object>> findNamespaces(Cluster cluster) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(NamespaceListRequest.newBuilder().build(), NamespaceListResponse.class)
      .thenApply(namespaceListResponse -> namespaceListResponse.getItemsList().stream()
        .map(this::assembleNamespaceInformation).toList())
      .thenApply(namespaces -> Map.of("success", true, "namespaces", namespaces));
  }

  private Map<String, Object> assembleNamespaceInformation(Namespace namespace) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", namespace.getName());
    information.put("status", namespace.getStatus());
    information.put("age", namespace.getAge());
    return information;
  }
}
