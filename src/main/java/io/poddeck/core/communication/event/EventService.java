package io.poddeck.core.communication.event;

import io.poddeck.common.EventReport;
import io.poddeck.common.Event;
import io.poddeck.core.cluster.ClusterEventRepository;
import io.poddeck.core.cluster.ClusterEvent;
import io.poddeck.core.communication.agent.Agent;
import io.poddeck.core.communication.service.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventService implements Service<EventReport> {
  private final ClusterEventRepository eventRepository;

  public void process(
    Agent agent, EventReport eventReport
  ) {
    eventRepository.generateAvailableId(UUID::randomUUID)
      .thenAccept(id -> storeEvent(agent, eventReport.getEvent(), id));
  }

  private void storeEvent(Agent agent, Event entry, UUID id) {
    var event = ClusterEvent.create(id, agent.cluster(), entry.getName(),
      entry.getNamespace(), entry.getReason(), entry.getMessage(),
      entry.getType(), entry.getInvolvedObjectKind(),
      entry.getInvolvedObjectName(), entry.getFirstTimestamp(),
      entry.getLastTimestamp(), entry.getCount());
    eventRepository.save(event);
  }
}