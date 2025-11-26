package io.poddeck.core.api.panel.pod;

import com.google.common.collect.Maps;
import io.poddeck.common.Pod;
import io.poddeck.common.PodContainerStatus;
import io.poddeck.common.PodListRequest;
import io.poddeck.common.PodListResponse;
import io.poddeck.core.api.panel.ClusterRestController;
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
public final class PodListController extends ClusterRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;

  private PodListController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
  }

  @RequestMapping(path = "/pods/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findPods(
    HttpServletRequest request
  ) {
    return findCluster(request).thenCompose(this::findPods);
  }

  public CompletableFuture<Map<String, Object>> findPods(Cluster cluster) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(PodListRequest.newBuilder().build(), PodListResponse.class)
      .thenApply(podListResponse -> podListResponse.getItemsList().stream()
        .map(this::assemblePodInformation).toList())
      .thenApply(pods -> Map.of("pods", pods));
  }

  private Map<String, Object> assemblePodInformation(Pod pod) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", pod.getMetadata().getName());
    information.put("namespace", pod.getMetadata().getNamespace());
    information.put("total_containers", pod.getSpec().getContainersCount());
    information.put("ready_containers", pod.getStatus().getStatusesList().stream()
      .filter(PodContainerStatus::getReady).count());
    information.put("status", pod.getStatus().getPhase());
    information.put("restarts", pod.getStatus().getStatusesList().stream()
      .mapToLong(PodContainerStatus::getRestartCount).sum());
    information.put("age", pod.getStatus().getAge());
    information.put("node", pod.getStatus().getNode());
    information.put("ip", pod.getStatus().getPodIp());
    return information;
  }
}