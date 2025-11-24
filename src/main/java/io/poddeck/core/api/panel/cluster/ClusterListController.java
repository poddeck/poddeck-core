package io.poddeck.core.api.panel.cluster;

import com.google.common.collect.Maps;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterListController extends ClusterRestController {
  private ClusterListController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  @RequestMapping(path = "/clusters/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusters() {
    return clusterRepository().findAll()
      .thenApply(clusters -> Map.of("clusters", clusters.stream()
        .map(this::assembleClusterInformation).toList()));
  }

  private Map<String, Object> assembleClusterInformation(Cluster cluster) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", cluster.id());
    information.put("name", cluster.name());
    return information;
  }
}
