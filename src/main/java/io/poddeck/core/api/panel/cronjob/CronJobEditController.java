package io.poddeck.core.api.panel.cronjob;

import io.poddeck.common.CronJobEditRequest;
import io.poddeck.common.CronJobEditResponse;
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
public final class CronJobEditController extends CronJobRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private CronJobEditController(
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
  @RequestMapping(path = "/cron-job/edit/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> editCronJob(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var cronJob = body.getString("cron_job");
    var raw = body.getString("raw");
    return findCluster(request).thenCompose(cluster ->
      editCronJob(cluster, namespace, cronJob, raw));
  }

  private CompletableFuture<Map<String, Object>> editCronJob(
    Cluster cluster, String namespace, String cronJob, String raw
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(CronJobEditRequest.newBuilder()
          .setNamespace(namespace).setCronJob(cronJob).setRaw(raw).build(),
        CronJobEditResponse.class)
      .thenApply(response -> processEditCronJobResult(cluster,
        cronJob, response));
  }

  private Map<String, Object> processEditCronJobResult(
    Cluster cluster, String cronJob, CronJobEditResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.cron-job.edit.notification.title",
        "panel.cron-job.edit.notification.description", cronJob);
    }
    return Map.of("success", response.getSuccess());
  }
}
