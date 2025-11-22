package io.poddeck.core.communication.service;

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.log.Log;
import io.poddeck.core.communication.agent.Agent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServiceRepository {
  private final Log log;
  private final Map<Class<? extends Message>, Service<?>> services = Maps.newConcurrentMap();

  /**
   * Registers a new service
   * @param messageClass The class of the message of the service
   * @param service The service that is to be registered
   */
  public <T extends Message> void register(
    Class<T> messageClass, Service<T> service
  ) {
    services.put(messageClass, service);
  }

  /**
   * Unregisters a service
   * @param messageClass The class of the message of the service that is to be unregistered
   */
  public <T extends Message> void unregister(Class<T> messageClass) {
    services.remove(messageClass);
  }

  /**
   * Is used to dispatch a message to a service
   * @param agent The agent that send the message
   * @param message The message to be dispatched
   */
  public void dispatch(Agent agent, TunnelMessage message) {
    try {
      var payload = message.getPayload();
      for (var messageClass : services.keySet()) {
        if (!payload.is(messageClass)) {
          continue;
        }
        var unpacked = payload.unpack(messageClass);
        var service = services.get(messageClass);
        ((Service<Message>) service).process(agent, unpacked);
      }
    } catch (Exception exception) {
      log.processError(exception);
    }
  }
}
