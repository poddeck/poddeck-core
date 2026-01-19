package io.poddeck.core.api.panel.cronjob;

import io.poddeck.common.CronJobRunRequest;
import io.poddeck.common.CronJobRunResponse;
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
public final class CronJobRunController extends CronJobRestController {
  private final AgentRegistry agentRegistry;
  private final AgentCommandFactory commandFactory;
  private final NotificationDispatch notificationDispatch;

  private CronJobRunController(
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
  @RequestMapping(path = "/cron-job/run/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> runCronJob(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var namespace = body.getString("namespace");
    var cronJob = body.getString("cron_job");
    return findCluster(request)
      .thenCompose(cluster -> runCronJob(cluster, namespace, cronJob));
  }

  private CompletableFuture<Map<String, Object>> runCronJob(
    Cluster cluster, String namespace, String cronJob
  ) {
    var agent = agentRegistry.findByCluster(cluster);
    if (agent.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return commandFactory.create(agent.get())
      .execute(CronJobRunRequest.newBuilder()
          .setNamespace(namespace).setCronJob(cronJob).build(),
        CronJobRunResponse.class)
      .thenApply(response -> processRunCronJobResult(cluster,
        cronJob, response));
  }

  private Map<String, Object> processRunCronJobResult(
    Cluster cluster, String cronJob, CronJobRunResponse response
  ) {
    if (response.getSuccess()) {
      notificationDispatch.dispatch(cluster.id(), NotificationType.REPORT,
        "panel.cron-job.run.notification.title",
        "panel.cron-job.run.notification.description", cronJob);
    }
    return Map.of("success", response.getSuccess());
  }
}
