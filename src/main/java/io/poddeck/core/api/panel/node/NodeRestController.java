package io.poddeck.core.api.panel.node;

import com.google.common.collect.Maps;
import io.poddeck.common.*;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.ClusterMetric;
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
public class NodeRestController extends ClusterRestController {
  protected NodeRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleNodeInformation(
    Node node, ClusterMetric metric
  ) {
    var capacity = node.getStatus().getCapacity();
    var information = Maps.<String, Object>newHashMap();
    information.put("name", node.getMetadata().getName());
    information.put("total_cpu_capacity", capacity.getTotalCpu());
    information.put("allocated_cpu_capacity", capacity.getAllocatedCpu());
    information.put("cpu_cores", metric.cpuCores());
    information.put("cpu_ratio", metric.cpuRatio());
    information.put("total_memory_capacity", capacity.getTotalMemory());
    information.put("allocated_memory_capacity", capacity.getAllocatedMemory());
    information.put("total_memory", metric.totalMemory());
    information.put("used_memory", metric.usedMemory());
    information.put("memory_ratio", metric.memoryRatio());
    information.put("total_storage", metric.totalStorage());
    information.put("used_storage", metric.usedStorage());
    information.put("storage_ratio", metric.storageRatio());
    information.put("architecture", node.getStatus().getInfo().getArchitecture());
    information.put("os_image", node.getStatus().getInfo().getOsImage());
    information.put("operating_system", node.getStatus().getInfo().getOperatingSystem());
    information.put("container_runtime_version",
      node.getStatus().getInfo().getContainerRuntimeVersion());
    information.put("kubelet_version", node.getStatus().getInfo().getKubeletVersion());
    information.put("ready", node.getStatus().getConditionsList().stream()
      .anyMatch(condition -> "Ready".equals(condition.getType()) &&
        "True".equals(condition.getStatus())));
    information.put("age", node.getStatus().getAge());
    information.put("labels", node.getMetadata().getLabelsMap());
    information.put("annotations", node.getMetadata().getAnnotationsMap());
    information.put("conditions", node.getStatus().getConditionsList()
      .stream().sorted(Comparator.comparingLong(NodeCondition::getLastTransition))
      .map(this::assembleConditionInformation).toList());
    information.put("events", node.getEventsList()
      .stream().map(this::assembleEventInformation).toList());
    return information;
  }

  private Map<String, Object> assembleConditionInformation(
    NodeCondition condition
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", condition.getType());
    information.put("status", condition.getStatus());
    information.put("reason", condition.getReason());
    information.put("message", condition.getMessage());
    information.put("last_heartbeat", condition.getLastHeartbeat());
    information.put("last_transition", condition.getLastTransition());
    return information;
  }

  private Map<String, Object> assembleEventInformation(NodeEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
