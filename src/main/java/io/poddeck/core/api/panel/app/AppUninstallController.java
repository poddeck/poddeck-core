package io.poddeck.core.api.panel.app;

import io.poddeck.common.AppInstallRequest;
import io.poddeck.common.AppInstallResponse;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.agent.command.AgentCommandFactory;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.notification.NotificationDispatch;
import io.poddeck.core.notification.NotificationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AppUninstallController extends AppRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private AppUninstallController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    AgentRegistry agentRegistry, AgentCommandFactory commandFactory,
    NotificationDispatch notificationDispatch
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.agentRegistry = agentRegistry;
    this.commandFactory = commandFactory;
    this.notificationDispatch = notificationDispatch;
  }

  @PanelEndpoint
  @RequestMapping(path = "/app/uninstall/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> uninstallApp(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getString("name");
    var chart = body.getString("chart");
    var namespace = body.getString("namespace");
    var version = body.getString("version");
    return findCluster(request)
      .thenCompose(cluster -> uninstallApp(cluster, name, chart, namespace, version));
  }

  private CompletableFuture<Map<String, Object>> uninstallApp(
    Cluster cluster, String name, String chart, String namespace, String version
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(AppInstallRequest.newBuilder().setName(name).setChart(chart)
          .setNamespace(namespace).setVersion(version).build(),
        AppInstallResponse.class)
      .thenApply(response -> processInstallAppResult(cluster, response, chart));
  }

  private Map<String, Object> processInstallAppResult(
    Cluster cluster, AppInstallResponse response, String chart
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.app.uninstall.notification.title",
        "panel.app.uninstall.notification.description", chart);
    }
    return Map.of("success", response.getSuccess(),
      "status", response.getStatus(),
      "output", response.getOutput());
  }
}
