package io.poddeck.core.api.panel.cluster;

import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterCreateController extends ClusterRestController {
  private ClusterCreateController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  @RequestMapping(path = "/cluster/create/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createCluster(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getString("name");
    var icon = body.getString("icon");
    return clusterRepository().generateAvailableId(UUID::randomUUID)
      .thenCompose(id -> createCluster(id, name, icon));
  }

  private CompletableFuture<Map<String, Object>> createCluster(
    UUID id, String name, String icon
  ) {
    var cluster = Cluster.create(id, name, icon, System.currentTimeMillis());
    return clusterRepository().save(cluster)
      .thenApply(_ -> Map.of("success", true, "cluster", cluster.id()));
  }
}
