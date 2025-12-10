package io.poddeck.core.api.panel.cluster;

import com.beust.jcommander.internal.Lists;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterEventRepository;
import io.poddeck.core.cluster.ClusterMetricRepository;
import io.poddeck.core.cluster.ClusterRepository;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterDeleteController extends ClusterRestController {
  private final ClusterMetricRepository metricRepository;
  private final ClusterEventRepository eventRepository;

  private ClusterDeleteController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    ClusterMetricRepository metricRepository,
    ClusterEventRepository eventRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.metricRepository = metricRepository;
    this.eventRepository = eventRepository;
  }

  @PanelEndpoint
  @RequestMapping(path = "/cluster/delete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> deleteCluster(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var id = body.getUUID("id");
    return clusterRepository().findById(id)
      .thenCompose(cluster -> deleteCluster(cluster));
  }

  private CompletableFuture<Map<String, Object>> deleteCluster(
    Optional<Cluster> clusterOptional
  ) {
    if (clusterOptional.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    var cluster = clusterOptional.get();
    var futures = Lists.<CompletableFuture<Void>>newArrayList();
    futures.add(clusterRepository().delete(cluster));
    futures.add(metricRepository.deleteByCluster(cluster.id()));
    futures.add(eventRepository.deleteByCluster(cluster.id()));
    return clusterRepository().delete(cluster)
      .thenApply(_ -> Map.of("success", true));
  }
}