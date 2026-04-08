package io.poddeck.core.api.panel;

import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
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

  protected CompletableFuture<Cluster> findCluster(HttpServletRequest request) {
    try {
      var clusterId = UUID.fromString(request.getHeader("Cluster"));
      return clusterRepository.findById(clusterId).thenApply(opt ->
        opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cluster not found")));
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or missing Cluster header");
    }
  }
}
