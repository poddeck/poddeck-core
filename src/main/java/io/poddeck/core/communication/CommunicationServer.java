package io.poddeck.core.communication;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.poddeck.common.log.Log;
import io.poddeck.core.communication.agent.command.AgentCommandRegistry;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.handshake.HandshakeService;
import io.poddeck.core.communication.service.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommunicationServer {
  private final Log log;
  private final CommunicationConfiguration configuration;
  private final AgentRegistry agentRegistry;
  private final AgentCommandRegistry agentCommandRegistry;
  private final ServiceRepository serviceRepository;
  private Server server;

  public void start() {
    try {
      server = ServerBuilder.forPort(configuration.port())
        .addService(TunnelService.create(log, agentRegistry,
          agentCommandRegistry, serviceRepository,
          HandshakeService.create(log, agentRegistry)))
        .build()
        .start();
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  public void close() throws Exception {
    if (server == null) {
      throw new Exception("Server never started");
    }
    server.shutdownNow();
  }
}
