package io.poddeck.core.api.panel.statefulset;

import com.google.common.collect.Maps;
import io.poddeck.common.StatefulSet;
import io.poddeck.common.StatefulSetCondition;
import io.poddeck.common.StatefulSetEvent;
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
public class StatefulSetRestController extends ClusterRestController {
  protected StatefulSetRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleStatefulSetInformation(StatefulSet statefulSet) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", statefulSet.getMetadata().getName());
    information.put("namespace", statefulSet.getMetadata().getNamespace());
    information.put("replicas", statefulSet.getStatus().getReplicas());
    information.put("ready_replicas", statefulSet.getStatus().getReadyReplicas());
    information.put("current_replicas",
      statefulSet.getStatus().getCurrentReplicas());
    information.put("updated_replicas",
      statefulSet.getStatus().getUpdatedReplicas());
    information.put("available_replicas",
      statefulSet.getStatus().getAvailableReplicas());
    information.put("current_revision",
      statefulSet.getStatus().getCurrentRevision());
    information.put("update_revision",
      statefulSet.getStatus().getUpdateRevision());
    information.put("age", statefulSet.getStatus().getAge());
    information.put("labels", statefulSet.getMetadata().getLabelsMap());
    information.put("annotations", statefulSet.getMetadata().getAnnotationsMap());
    information.put("conditions", statefulSet.getStatus().getConditionsList().stream()
      .sorted(Comparator.comparingLong(StatefulSetCondition::getLastTransitionTime))
      .map(this::assembleConditionInformation).toList());
    information.put("events", statefulSet.getEventsList()
      .stream().map(this::assembleEventInformation).toList());
    information.put("raw", statefulSet.getRaw());
    information.putAll(assembleContainerInformation(statefulSet));
    return information;
  }

  private Map<String, Object> assembleContainerInformation(StatefulSet statefulSet) {
    var template = statefulSet.getSpec().getTemplate().getSpec();
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
    StatefulSetCondition condition
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", condition.getType());
    information.put("status", condition.getStatus());
    information.put("reason", condition.getReason());
    information.put("message", condition.getMessage());
    information.put("last_transition", condition.getLastTransitionTime());
    return information;
  }

  private Map<String, Object> assembleEventInformation(StatefulSetEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
