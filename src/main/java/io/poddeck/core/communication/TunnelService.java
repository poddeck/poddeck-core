package io.poddeck.core.communication;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.poddeck.common.HandshakeRequest;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.TunnelServiceGrpc;
import io.poddeck.common.log.Log;
import io.poddeck.core.communication.agent.Agent;
import io.poddeck.core.communication.agent.command.AgentCommandRegistry;
import io.poddeck.core.communication.agent.AgentRegistry;
import io.poddeck.core.communication.handshake.HandshakeService;
import io.poddeck.core.communication.service.ServiceRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public class TunnelService extends TunnelServiceGrpc.TunnelServiceImplBase {
  private final Log log;
  private final AgentRegistry agentRegistry;
  private final AgentCommandRegistry agentCommandRegistry;
  private final ServiceRepository serviceRepository;
  private final HandshakeService handshakeService;

  @Override
  public StreamObserver<TunnelMessage> connect(
    StreamObserver<TunnelMessage> stream
  ) {
    return new StreamObserver<>() {
      @Override
      public void onNext(TunnelMessage message) {
        var payload = message.getPayload();
        if (payload.is(HandshakeRequest.class)) {
          try {
            handshakeService.process(stream, payload.unpack(HandshakeRequest.class));
          } catch (Exception exception) {
            log.processError(exception);
          }
          return;
        }
        agentRegistry.findByStream(stream)
          .ifPresent(agent -> processMessage(agent, message));
      }

      private void processMessage(Agent agent, TunnelMessage message) {
        var optionalCommand = agentCommandRegistry.findById(message.getRequestId());
        if (optionalCommand.isPresent()) {
          optionalCommand.get().complete(message);
          return;
        }
        serviceRepository.dispatch(agent, message);
      }

      @Override
      public void onError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException) {
          processDisconnect(stream);
          return;
        }
        log.processError(throwable);
      }

      @Override
      public void onCompleted() {
        stream.onCompleted();
        processDisconnect(stream);
      }
    };
  }

  private void processDisconnect(StreamObserver<TunnelMessage> stream) {
    var agentOptional = agentRegistry.findByStream(stream);
    if (agentOptional.isEmpty()) {
      return;
    }
    var agent = agentOptional.get();
    log.info("Agent " + agent.cluster() + " disconnected");
    agentRegistry.unregister(agent);
  }
}
