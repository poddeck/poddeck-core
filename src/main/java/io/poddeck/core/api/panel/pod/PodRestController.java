package io.poddeck.core.api.panel.pod;

import com.google.common.collect.Maps;
import io.poddeck.common.Container;
import io.poddeck.common.Pod;
import io.poddeck.common.PodContainerStatus;
import io.poddeck.common.PodEvent;
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
public class PodRestController extends ClusterRestController {
  protected PodRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assemblePodInformation(Pod pod) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", pod.getMetadata().getName());
    information.put("namespace", pod.getMetadata().getNamespace());
    information.put("total_containers", pod.getSpec().getContainersCount());
    information.put("ready_containers", pod.getStatus().getStatusesList().stream()
      .filter(PodContainerStatus::getReady).count());
    information.put("status", pod.getStatus().getPhase());
    information.put("restarts", pod.getStatus().getStatusesList().stream()
      .mapToLong(PodContainerStatus::getRestartCount).sum());
    information.put("age", pod.getStatus().getAge());
    information.put("node", pod.getStatus().getNode());
    information.put("pod_ip", pod.getStatus().getPodIp());
    information.put("host_ip", pod.getStatus().getHostIp());
    information.put("labels", pod.getMetadata().getLabelsMap());
    information.put("annotations", pod.getMetadata().getAnnotationsMap());
    information.put("containers", pod.getSpec().getContainersList().stream()
      .map(container -> assembleContainerInformation(container,
        pod.getStatus().getStatusesList().stream()
          .filter(status -> status.getName().equals(container.getName()))
          .findFirst().get()))
      .toList());
    information.put("events", pod.getEventsList().stream()
      .map(this::assembleEventInformation).toList());
    return information;
  }

  private Map<String, Object> assembleContainerInformation(
    Container container, PodContainerStatus status
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", container.getName());
    information.put("image", container.getImage());
    information.put("ready", status.getReady());
    information.put("state", status.getState());
    information.put("restarts", status.getRestartCount());
    return information;
  }

  private Map<String, Object> assembleEventInformation(PodEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
