package io.poddeck.core.api.panel.event;

import com.google.common.collect.Maps;
import io.poddeck.core.api.panel.ClusterRestController;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.cluster.ClusterEvent;
import io.poddeck.core.cluster.ClusterEventRepository;
import io.poddeck.core.cluster.ClusterRepository;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class EventController extends ClusterRestController {
  private final ClusterEventRepository eventRepository;

  private EventController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, ClusterRepository clusterRepository,
    ClusterEventRepository eventRepository
  ) {
    super(authenticationKey, memberRepository, clusterRepository);
    this.eventRepository = eventRepository;
  }

  @PanelEndpoint
  @RequestMapping(path = "/events/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findEvents(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var start = body.getLong("start");
    var end = body.getLong("end");
    var limit = body.getInt("limit");
    return findCluster(request)
      .thenCompose(cluster -> findEvents(cluster, start, end, limit));
  }

  public CompletableFuture<Map<String, Object>> findEvents(
    Cluster cluster, long start, long end, int limit
  ) {
    var pageable = PageRequest.of(0, limit);
    return eventRepository
      .findEventsByClusterAndTimeRange(cluster.id(), start, end, pageable)
      .thenApply(events -> events.stream()
        .map(this::assembleEventInformation).toList())
      .thenApply(events -> Map.of("success", true, "events", events));
  }

  private Map<String, Object> assembleEventInformation(
    ClusterEvent event
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", event.name());
    information.put("namespace", event.namespace());
    information.put("reason", event.reason());
    information.put("message", event.message());
    information.put("type", event.type());
    information.put("involved_kind", event.involvedKind());
    information.put("involved_name", event.involvedName());
    information.put("first_timestamp", event.firstTimestamp());
    information.put("last_timestamp", event.lastTimestamp());
    information.put("count", event.count());
    return information;
  }
}