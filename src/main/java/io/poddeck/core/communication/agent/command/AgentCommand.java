package io.poddeck.core.communication.agent.command;

import com.google.protobuf.Message;
import io.poddeck.common.TunnelMessage;
import io.poddeck.common.log.Log;
import io.poddeck.core.communication.agent.Agent;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class AgentCommand {
  private final Log log;
  private final AgentCommandRegistry agentCommandRegistry;
  private final Agent agent;

  public <T extends Message> CompletableFuture<T> execute(
    Message message, Class<T> responseClass
  ) {
    var futureResponse = new CompletableFuture<TunnelMessage>();
    var id = UUID.randomUUID().toString();
    agentCommandRegistry.register(id, futureResponse);
    agent.send(id, message);
    return futureResponse
      .thenApply(response -> processResponse(id, response, responseClass));
  }

  private <T extends Message> T processResponse(
    String id, TunnelMessage response, Class<T> responseClass
  ) {
    agentCommandRegistry.unregister(id);
    var payload = response.getPayload();
    if (!payload.is(responseClass)) {
      return null;
    }
    try {
      return payload.unpack(responseClass);
    } catch (Exception exception) {
      log.processError(exception);
      return null;
    }
  }
}
