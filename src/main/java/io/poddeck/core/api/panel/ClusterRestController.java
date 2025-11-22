package io.poddeck.core.api.panel;

import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
public class ClusterRestController extends PanelRestController {
  private final ClusterRepository clusterRepository;

  protected ClusterRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository);
    this.clusterRepository = clusterRepository;
  }

  /**
   * Is used to find the member that send the request
   * @param request The request
   * @return A future that contains the member
   */
  protected CompletableFuture<Cluster> findCluster(HttpServletRequest request) {
    try {
      var clusterId = UUID.fromString(request.getHeader("Cluster"));
      return clusterRepository.findById(clusterId).thenApply(Optional::get);
    } catch (Exception exception) {
      return CompletableFuture.completedFuture(null);
    }
  }
}
