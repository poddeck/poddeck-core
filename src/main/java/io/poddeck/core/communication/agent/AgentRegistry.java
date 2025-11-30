package io.poddeck.core.communication.agent;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import io.poddeck.common.TunnelMessage;
import io.poddeck.core.cluster.Cluster;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
   * Is used to find an agent by a {@link Cluster}
   * @param cluster The {@link Cluster}
   * @return The agent if it could be found
   */
  public Optional<Agent> findByCluster(Cluster cluster) {
    if (cluster == null) {
      return Optional.empty();
    }
    return findByCluster(cluster.id());
  }

  /**
   * Is used to find an agent by the cluster id
   * @param cluster The id of the cluster
   * @return The agent if it could be found
   */
  public Optional<Agent> findByCluster(UUID cluster) {
    return agents.stream()
      .filter(agent -> agent.cluster().equals(cluster))
      .findFirst();
  }

  /**
   * Is used to check whether an agent by a {@link Cluster} exists
   * @param cluster The {@link Cluster}
   * @return Whether the cluster agent exists
   */
  public boolean existsByCluster(Cluster cluster) {
    if (cluster == null) {
      return false;
    }
    return existsByCluster(cluster.id());
  }

  /**
   * Is used to check whether an agent by the cluster exists
   * @param cluster The id of the cluster
   * @return Whether the cluster agent exists
   */
  public boolean existsByCluster(UUID cluster) {
    return agents.stream()
      .anyMatch(agent -> agent.cluster().equals(cluster));
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
