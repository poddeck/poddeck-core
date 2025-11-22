package io.poddeck.core.communication.agent;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import io.poddeck.common.TunnelMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgentRegistry {
  private final List<Agent> agents = Lists.newArrayList();

  /**
   * Registers a new agent
   * @param agent The agent that is to be registered
   */
  public void register(Agent agent) {
    agents.add(agent);
  }

  /**
   * Unregisters an agent
   * @param agent The agent that is to be unregistered
   */
  public void unregister(Agent agent) {
    agents.remove(agent);
  }

  /**
   * Is used to find an agent by tunnel stream
   * @param stream The tunnel stream of the agent
   * @return The agent if it could be found
   */
  public Optional<Agent> findByStream(StreamObserver<TunnelMessage> stream) {
    return agents.stream()
      .filter(agent -> agent.stream().equals(stream))
      .findFirst();
  }

  /**
   * Lists all registered agent
   * @return The agents
   */
  public List<Agent> findAll() {
    return List.copyOf(agents);
  }
}
