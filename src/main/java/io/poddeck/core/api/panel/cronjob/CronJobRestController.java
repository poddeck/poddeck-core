package io.poddeck.core.api.panel.cronjob;

import com.google.common.collect.Maps;
import io.poddeck.common.CronJob;
import io.poddeck.common.CronJobEvent;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.Map;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
public class CronJobRestController extends ClusterRestController {
  protected CronJobRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleCronJobInformation(CronJob cronJob) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", cronJob.getMetadata().getName());
    information.put("namespace", cronJob.getMetadata().getNamespace());
    information.put("schedule", cronJob.getSpec().getSchedule());
    information.put("time_zone", cronJob.getSpec().getTimeZone());
    information.put("suspend", cronJob.getSpec().getSuspend());
    information.put("concurrency_policy", cronJob.getSpec().getConcurrencyPolicy());
    information.put("successful_jobs_history_limit",
      cronJob.getSpec().getSuccessfulJobsHistoryLimit());
    information.put("failed_jobs_history_limit",
      cronJob.getSpec().getFailedJobsHistoryLimit());
    information.put("active", cronJob.getStatus().getActive());
    information.put("last_schedule_time", cronJob.getStatus().getLastScheduleTime());
    information.put("last_successful_time", cronJob.getStatus().getLastSuccessfulTime());
    information.put("age", cronJob.getStatus().getAge());
    information.put("labels", cronJob.getMetadata().getLabelsMap());
    information.put("annotations", cronJob.getMetadata().getAnnotationsMap());
    information.put("events", cronJob.getEventsList()
      .stream().map(this::assembleEventInformation).toList());
    information.put("raw", cronJob.getRaw());
    return information;
  }

  private Map<String, Object> assembleEventInformation(CronJobEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
