package io.poddeck.core.api.panel.cluster;

import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.communication.CommunicationConfiguration;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterCreateController extends ClusterRestController {
  private final CommunicationConfiguration communicationConfiguration;

  private ClusterCreateController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    CommunicationConfiguration communicationConfiguration
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.communicationConfiguration = communicationConfiguration;
  }

  @PanelEndpoint
  @RequestMapping(path = "/cluster/create/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createCluster(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getSanitizedString("name");
    var icon = body.getString("icon");
    return clusterRepository().generateAvailableId(UUID::randomUUID)
      .thenCompose(id -> createCluster(id, name, icon));
  }

  private CompletableFuture<Map<String, Object>> createCluster(
    UUID id, String name, String icon
  ) {
    var agentKey = generateAgentKey();
    var cluster = Cluster.create(id, name, icon, agentKey, System.currentTimeMillis());
    return clusterRepository().save(cluster)
      .thenApply(_ -> Map.of("success", true, "cluster", cluster.id(),
        "agent_key", agentKey));
  }

  private String generateAgentKey() {
    var bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
