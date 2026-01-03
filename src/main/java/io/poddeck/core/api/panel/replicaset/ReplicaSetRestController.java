package io.poddeck.core.api.panel.replicaset;

import com.google.common.collect.Maps;
import io.poddeck.common.ReplicaSet;
import io.poddeck.common.ReplicaSetCondition;
import io.poddeck.common.ReplicaSetEvent;
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
public class ReplicaSetRestController extends ClusterRestController {
  protected ReplicaSetRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleReplicaSetInformation(ReplicaSet replicaSet) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", replicaSet.getMetadata().getName());
    information.put("namespace", replicaSet.getMetadata().getNamespace());
    information.put("replicas", replicaSet.getStatus().getReplicas());
    information.put("fully_labeled_replicas",
      replicaSet.getStatus().getFullyLabeledReplicas());
    information.put("ready_replicas", replicaSet.getStatus().getReadyReplicas());
    information.put("available_replicas",
      replicaSet.getStatus().getAvailableReplicas());
    information.put("observed_generation",
      replicaSet.getStatus().getObservedGeneration());
    information.put("age", replicaSet.getStatus().getAge());
    information.put("labels", replicaSet.getMetadata().getLabelsMap());
    information.put("annotations", replicaSet.getMetadata().getAnnotationsMap());
    information.put("conditions", replicaSet.getStatus().getConditionsList().stream()
      .sorted(Comparator.comparingLong(ReplicaSetCondition::getLastTransitionTime))
      .map(this::assembleConditionInformation).toList());
    information.put("events", replicaSet.getEventsList()
      .stream().map(this::assembleEventInformation).toList());
    information.put("raw", replicaSet.getRaw());
    information.putAll(assembleContainerInformation(replicaSet));
    return information;
  }

  private Map<String, Object> assembleContainerInformation(ReplicaSet replicaSet) {
    var template = replicaSet.getSpec().getTemplate().getSpec();
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
    ReplicaSetCondition condition
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", condition.getType());
    information.put("status", condition.getStatus());
    information.put("reason", condition.getReason());
    information.put("message", condition.getMessage());
    information.put("last_transition", condition.getLastTransitionTime());
    return information;
  }

  private Map<String, Object> assembleEventInformation(ReplicaSetEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
