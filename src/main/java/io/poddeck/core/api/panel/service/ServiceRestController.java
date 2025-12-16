package io.poddeck.core.api.panel.service;

import com.google.common.collect.Maps;
import io.poddeck.common.Service;
import io.poddeck.common.ServiceEvent;
import io.poddeck.common.ServicePort;
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
public class ServiceRestController extends ClusterRestController {
  protected ServiceRestController(
    Key authenticationKey,
    MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleServiceInformation(Service service) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", service.getMetadata().getName());
    information.put("namespace", service.getMetadata().getNamespace());
    information.put("type", service.getSpec().getType());
    information.put("age", service.getStatus().getAge());
    information.put("cluster_ip", service.getSpec().getClusterIp());
    information.put("cluster_ips", service.getSpec().getClusterIpsList());
    information.put("ip_family_policy", service.getSpec().getIpFamilyPolicy());
    information.put("ip_families", service.getSpec().getIpFamiliesList());
    information.put("selector", service.getSpec().getSelectorMap());
    information.put("session_affinity", service.getSpec().getSessionAffinity());
    information.put("internal_traffic_policy",
      service.getSpec().getInternalTrafficPolicy());
    information.put("external_traffic_policy",
      service.getSpec().getExternalTrafficPolicy());
    information.put("labels", service.getMetadata().getLabelsMap());
    information.put("annotations", service.getMetadata().getAnnotationsMap());
    information.put("ports", service.getSpec().getPortsList()
      .stream()
      .map(this::assemblePortInformation)
      .toList());
    information.put("endpoints", service.getStatus().getEndpointsList()
      .stream()
      .map(e -> Map.<String, Object>of(
        "ip", e.getIp(),
        "port", e.getPort()
      ))
      .toList());
    information.put("events", service.getEventsList()
      .stream()
      .sorted(Comparator.comparingLong(ServiceEvent::getTimestamp))
      .map(this::assembleEventInformation)
      .toList());
    information.put("raw", service.getRaw());
    return information;
  }

  private Map<String, Object> assemblePortInformation(ServicePort port) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", port.getName());
    information.put("port", port.getPort());
    information.put("protocol", port.getProtocol());
    information.put("target_port", port.getTargetPort());
    information.put("node_port", port.getNodePort());
    return information;
  }

  private Map<String, Object> assembleEventInformation(ServiceEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
