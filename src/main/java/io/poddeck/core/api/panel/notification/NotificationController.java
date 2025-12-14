package io.poddeck.core.api.panel.notification;

import com.google.common.collect.Maps;
import io.poddeck.common.iterator.AsyncIterator;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.locale.Translation;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.notification.Notification;
import io.poddeck.core.notification.NotificationRepository;
import io.poddeck.core.notification.NotificationState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public final class NotificationController extends ClusterRestController {
  private final NotificationRepository notificationRepository;
  private final Translation translation;

  private NotificationController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    NotificationRepository notificationRepository, Translation translation
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.notificationRepository = notificationRepository;
    this.translation = translation;
  }

  @PanelEndpoint
  @RequestMapping(path = "/notifications/all/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findAllNotifications(
    HttpServletRequest request
  ) {
    return findMember(request)
      .thenCompose(member -> notificationRepository.findAllByMember(member.id())
        .thenCompose(notifications -> findClusters(notifications)
          .thenApply(clusters -> Map.of("notifications", notifications.stream()
            .sorted(Comparator.comparing(Notification::createdAt).reversed())
            .map(notification -> assembleNotificationInformation(member,
              notification, clusters))
            .toList()))));
  }

  private CompletableFuture<List<Optional<Cluster>>> findClusters(
    List<Notification> notifications
  ) {
    var clusterIds = notifications.stream().map(Notification::cluster)
      .distinct().toList();
    return AsyncIterator.execute(clusterIds, clusterRepository()::findById);
  }

  @PanelEndpoint
  @RequestMapping(path = "/notifications/cluster/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusterNotifications(
    HttpServletRequest request
  ) {
    return findMember(request)
      .thenCompose(member -> findCluster(request)
        .thenCompose(cluster -> findClusterNotifications(member, cluster)));
  }

  private CompletableFuture<Map<String, Object>> findClusterNotifications(
    Member member, Cluster cluster
  ) {
    return notificationRepository.findAllByMemberAndCluster(member.id(), cluster.id())
      .thenApply(notifications -> Map.of("notifications", notifications.stream()
        .sorted(Comparator.comparing(Notification::createdAt).reversed())
        .map(notification -> assembleNotificationInformation(member,
          notification, cluster))
        .toList()));
  }

  private Map<String, Object> assembleNotificationInformation(
    Member member, Notification notification, List<Optional<Cluster>> clusters
  ) {
    var cluster = clusters.stream().flatMap(Optional::stream)
      .filter(entry -> entry.id().equals(notification.cluster())).findFirst();
    return assembleNotificationInformation(member, notification, cluster);
  }

  private Map<String, Object> assembleNotificationInformation(
    Member member, Notification notification, Cluster cluster
  ) {
    return assembleNotificationInformation(member, notification,
      Optional.of(cluster));
  }

  private Map<String, Object> assembleNotificationInformation(
    Member member, Notification notification, Optional<Cluster> cluster
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", notification.id());
    var parameters = notification.parameters().toArray(String[]::new);
    information.put("title", translation.translateMember(member,
      notification.title(), parameters));
    information.put("description", translation.translateMember(member,
      notification.description(), parameters));
    information.put("type", notification.type());
    information.put("state", notification.state());
    information.put("created_at", notification.createdAt());
    information.put("cluster_found", cluster.isPresent());
    if (cluster.isPresent()) {
      information.put("cluster_name", cluster.get().name());
      information.put("cluster_icon", cluster.get().icon());
    }
    return information;
  }

  @PanelEndpoint
  @RequestMapping(path = "/notification/seen/", method = RequestMethod.POST)
  public CompletableFuture<Void> makeNotificationSeen(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var notificationId = body.getUUID("notification");
    return notificationRepository.findById(notificationId)
      .thenCompose(notification -> makeNotificationSeen(notification,
        findMemberId(request)));
  }

  private CompletableFuture<Void> makeNotificationSeen(
    Optional<Notification> notificationOptional, UUID memberId
  ) {
    if (notificationOptional.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    var notification = notificationOptional.get();
    if (!notification.member().equals(memberId)) {
      return CompletableFuture.completedFuture(null);
    }
    notification.updateState(NotificationState.SEEN);
    return notificationRepository.save(notification).thenApply(_ -> null);
  }
}