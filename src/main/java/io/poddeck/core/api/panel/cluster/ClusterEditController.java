package io.poddeck.core.api.panel.cluster;

import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
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
public final class ClusterEditController extends ClusterRestController {
  private ClusterEditController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  @PanelEndpoint
  @RequestMapping(path = "/cluster/edit/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> editCluster(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var id = body.getUUID("id");
    var name = body.getSanitizedString("name");
    var icon = body.getString("icon");
    return clusterRepository().findById(id)
      .thenCompose(cluster -> editCluster(cluster, name, icon));
  }

  private CompletableFuture<Map<String, Object>> editCluster(
    Optional<Cluster> clusterOptional, String name, String icon
  ) {
    if (clusterOptional.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    var cluster = clusterOptional.get();
    cluster.changeName(name);
    cluster.changeIcon(icon);
    return clusterRepository().save(cluster)
      .thenApply(_ -> Map.of("success", true));
  }
}