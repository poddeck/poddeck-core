package io.poddeck.core.communication.agent.command;

import com.google.common.collect.Maps;
import io.poddeck.common.TunnelMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgentCommandRegistry {
  private final Map<String, CompletableFuture<TunnelMessage>> commands =
    Maps.newConcurrentMap();

  /**
   * Registers a new agent command
   * @param id The id of the agent command
   * @param future The future response
   */
  public void register(String id, CompletableFuture<TunnelMessage> future) {
    commands.put(id, future);
  }

  /**
   * Unregisters an agent command
   * @param id The id of the agent command
   */
  public void unregister(String id) {
    commands.remove(id);
  }

  /**
   * Is used to find an agent command by id
   * @param id The id of the command
   * @return The future response if it could be found
   */
  public Optional<CompletableFuture<TunnelMessage>> findById(String id) {
    return commands.entrySet().stream()
      .filter(entry -> entry.getKey().equals(id))
      .map(Map.Entry::getValue)
      .findFirst();
  }
}
