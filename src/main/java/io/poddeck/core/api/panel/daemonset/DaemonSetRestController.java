package io.poddeck.core.api.panel.daemonset;

import com.google.common.collect.Maps;
import io.poddeck.common.DaemonSet;
import io.poddeck.common.DaemonSetCondition;
import io.poddeck.common.DaemonSetEvent;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.Comparator;
import java.util.Map;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
public class DaemonSetRestController extends ClusterRestController {
  protected DaemonSetRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleDaemonSetInformation(DaemonSet daemonSet) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", daemonSet.getMetadata().getName());
    information.put("namespace", daemonSet.getMetadata().getNamespace());
    information.put("current_number_scheduled",
      daemonSet.getStatus().getCurrentNumberScheduled());
    information.put("number_misscheduled",
      daemonSet.getStatus().getNumberMisscheduled());
    information.put("desired_number_scheduled",
      daemonSet.getStatus().getDesiredNumberScheduled());
    information.put("number_ready", daemonSet.getStatus().getNumberReady());
    information.put("updated_number_scheduled",
      daemonSet.getStatus().getUpdatedNumberScheduled());
    information.put("number_available", daemonSet.getStatus().getNumberAvailable());
    information.put("number_unavailable", daemonSet.getStatus().getNumberUnavailable());
    information.put("updated_revision", daemonSet.getStatus().getUpdatedRevision());
    information.put("current_revision", daemonSet.getStatus().getCurrentRevision());
    information.put("age", daemonSet.getStatus().getAge());
    information.put("labels", daemonSet.getMetadata().getLabelsMap());
    information.put("annotations", daemonSet.getMetadata().getAnnotationsMap());
    information.put("conditions", daemonSet.getStatus().getConditionsList()
      .stream().sorted(Comparator.comparingLong(DaemonSetCondition::getLastUpdate))
      .map(this::assembleConditionInformation).toList());
    information.put("events", daemonSet.getEventsList()
      .stream().map(this::assembleEventInformation).toList());
    information.put("raw", daemonSet.getRaw());
    information.putAll(assembleContainerInformation(daemonSet));
    return information;
  }

  private Map<String, Object> assembleContainerInformation(DaemonSet daemonSet) {
    var template = daemonSet.getSpec().getTemplate().getSpec();
    var primary = template.getContainersCount() > 0 ?
      template.getContainers(0) : null;
    var resources = primary != null ? primary.getResources() : null;
    var information = Maps.<String, Object>newHashMap();
    information.put("container_name", primary != null ? primary.getName() : "");
    information.put("container_image", primary != null ? primary.getImage() : "");
    information.put("container_cpu_limit", resources != null ?
      resources.getCpuLimit() : -1);
    information.put("container_cpu_request", resources != null ?
      resources.getCpuRequest() : -1);
    information.put("container_memory_limit", resources != null ?
      resources.getMemoryLimit() : -1);
    information.put("container_memory_request", resources != null ?
      resources.getMemoryRequest() : -1);
    return information;
  }

  private Map<String, Object> assembleConditionInformation(
    DaemonSetCondition condition
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", condition.getType());
    information.put("status", condition.getStatus());
    information.put("reason", condition.getReason());
    information.put("message", condition.getMessage());
    information.put("last_update", condition.getLastUpdate());
    return information;
  }

  private Map<String, Object> assembleEventInformation(DaemonSetEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
