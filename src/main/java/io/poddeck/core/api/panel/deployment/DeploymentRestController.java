package io.poddeck.core.api.panel.deployment;

import com.google.common.collect.Maps;
import io.poddeck.common.Deployment;
import io.poddeck.common.DeploymentCondition;
import io.poddeck.common.DeploymentEvent;
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
public class DeploymentRestController extends ClusterRestController {
  protected DeploymentRestController(
    Key authenticationKey, MemberRepository memberRepository,
    ClusterRepository clusterRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
  }

  protected Map<String, Object> assembleDeploymentInformation(Deployment deployment) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", deployment.getMetadata().getName());
    information.put("namespace", deployment.getMetadata().getNamespace());
    information.put("replicas", deployment.getStatus().getReplicas());
    information.put("updated_replicas",
      deployment.getStatus().getUpdatedReplicas());
    information.put("ready_replicas", deployment.getStatus().getReadyReplicas());
    information.put("available_replicas",
      deployment.getStatus().getAvailableReplicas());
    information.put("unavailable_replicas",
      deployment.getStatus().getUnavailableReplicas());
    information.put("age", deployment.getStatus().getAge());
    information.put("labels", deployment.getMetadata().getLabelsMap());
    information.put("annotations", deployment.getMetadata().getAnnotationsMap());
    var template = deployment.getSpec().getTemplate().getSpec();
    var primary = template.getContainersCount() > 0 ?
      template.getContainers(0) : null;
    information.put("container_name", primary != null ? primary.getName() : "");
    information.put("container_image", primary != null ? primary.getImage() : "");
    information.put("replica_set", deployment.getStatus().getReplicaSet());
    information.put("conditions", deployment.getStatus().getConditionsList()
      .stream().sorted(Comparator.comparingLong(DeploymentCondition::getLastUpdate))
      .map(this::assembleConditionInformation).toList());
    information.put("events", deployment.getEventsList()
      .stream().map(this::assembleEventInformation).toList());
    information.put("raw", deployment.getRaw());
    return information;
  }

  private Map<String, Object> assembleConditionInformation(
    DeploymentCondition condition
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", condition.getType());
    information.put("status", condition.getStatus());
    information.put("reason", condition.getReason());
    information.put("message", condition.getMessage());
    information.put("last_update", condition.getLastUpdate());
    return information;
  }

  private Map<String, Object> assembleEventInformation(DeploymentEvent event) {
    var information = Maps.<String, Object>newHashMap();
    information.put("type", event.getType());
    information.put("reason", event.getReason());
    information.put("message", event.getMessage());
    information.put("timestamp", event.getTimestamp());
    information.put("source", event.getSource());
    return information;
  }
}
