package io.poddeck.core.communication.handshake;

import io.grpc.stub.StreamObserver;
import io.poddeck.common.HandshakeRequest;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.log.Log;
import io.poddeck.core.communication.agent.Agent;
import io.poddeck.core.communication.agent.AgentRegistry;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor(staticName = "create")
public final class HandshakeService {
  private final Log log;
  private final AgentRegistry agentRegistry;

  public void process(
    StreamObserver<TunnelMessage> stream, HandshakeRequest handshakeRequest
  ) {
    log.info("Handshaking with cluster " + handshakeRequest.getCluster());
    //TODO: CHECK CLUSTER ID & KEY
    var agent = Agent.create(UUID.fromString(handshakeRequest.getCluster()), stream);
    agentRegistry.register(agent);
  }
}