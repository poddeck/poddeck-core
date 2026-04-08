package io.poddeck.core.communication.handshake;

import io.grpc.stub.StreamObserver;
import io.poddeck.common.HandshakeRequest;
import io.poddeck.common.HandshakeResponse;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.log.Log;
import io.poddeck.core.cluster.Cluster;
import io.poddeck.core.communication.agent.Agent;
import io.poddeck.core.communication.agent.AgentRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor(staticName = "create")
public final class HandshakeService {
  private final Log log;
  private final AgentRegistry agentRegistry;
  private final EntityManagerFactory entityManagerFactory;

  public void process(
    StreamObserver<TunnelMessage> stream, HandshakeRequest handshakeRequest
  ) {
    log.info("Handshaking with cluster " + handshakeRequest.getCluster());
    var clusterId = UUID.fromString(handshakeRequest.getCluster());
    var key = handshakeRequest.getKey();
    try {
      var entityManager = entityManagerFactory.createEntityManager();
      var cluster = entityManager.find(Cluster.class, clusterId);
      entityManager.close();
      if (cluster == null || !cluster.agentKey().equals(key)) {
        log.warning("Handshake rejected for cluster " + clusterId + ": invalid cluster ID or key");
        return;
      }
    } catch (Exception exception) {
      log.warning("Handshake failed for cluster " + clusterId + ": " + exception.getMessage());
      return;
    }
    var existingAgentOptional = agentRegistry.findByCluster(clusterId);
    if (existingAgentOptional.isPresent()) {
      var existingAgent = existingAgentOptional.get();
      existingAgent.stream().onCompleted();
      agentRegistry.unregister(existingAgent);
    }
    var agent = Agent.create(clusterId, stream);
    agentRegistry.register(agent);
  }
}