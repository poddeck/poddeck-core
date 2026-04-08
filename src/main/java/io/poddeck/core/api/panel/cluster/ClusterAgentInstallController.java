package io.poddeck.core.api.panel.cluster;

import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
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
public final class ClusterAgentInstallController extends ClusterRestController {
  private ClusterAgentInstallController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  @PanelEndpoint
  @RequestMapping(path = "/cluster/agent-install/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> getAgentInstall(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var clusterId = UUID.fromString(body.getString("id"));
    return clusterRepository().findById(clusterId)
      .thenApply(clusterOptional -> {
        if (clusterOptional.isEmpty()) {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          return Map.of("success", false);
        }
        var cluster = clusterOptional.get();
        return Map.<String, Object>of(
          "success", true,
          "cluster_id", cluster.id().toString(),
          "agent_key", cluster.agentKey()
        );
      });
  }
}
