package io.poddeck.core.communication;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommunicationServer {
  private final Log log;
  private final CommunicationConfiguration configuration;
  private Server server;

  public void startAsync() {
    new Thread(this::start).start();
  }

  public void start() {
    try {
      server = ServerBuilder.forPort(configuration.port())
        .addService(HandshakeService.create()).build().start();
      server.awaitTermination();
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
