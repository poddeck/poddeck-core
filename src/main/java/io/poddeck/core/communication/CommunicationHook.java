package io.poddeck.core.communication;

import io.poddeck.common.*;
import io.poddeck.common.event.EventHook;
import io.poddeck.common.event.Hook;
import io.poddeck.core.application.ApplicationLaunchEvent;
import io.poddeck.core.communication.event.EventService;
import io.poddeck.core.communication.metric.MetricService;
import io.poddeck.core.communication.service.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommunicationHook implements Hook {
  private final ServiceRepository serviceRepository;
  private final MetricService metricService;
  private final EventService eventService;

  @EventHook
  private void applicationLaunch(ApplicationLaunchEvent event) {
    serviceRepository.register(MetricReport.class, metricService);
    serviceRepository.register(EventReport.class, eventService);
  }
}