package io.poddeck.core.api.panel.node;

import com.google.common.collect.Maps;
import io.poddeck.common.Node;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.cluster.ClusterMetric;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.security.Key;
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
    information.put("version", node.getStatus().getInfo().getKubeletVersion());
    information.put("ready", node.getStatus().getCondition().getIsReady());
    return information;
  }
}
