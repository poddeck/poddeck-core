package io.poddeck.core.communication.agent;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.poddeck.common.TunnelMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class Agent {
  @Getter
  private final UUID cluster;
  @Getter
  private final StreamObserver<TunnelMessage> stream;

  public void send(Message message) {
    send("", message);
  }

  public void send(String requestId, Message message) {
    var tunnelMessage = TunnelMessage.newBuilder()
      .setPayload(Any.pack(message));
    if (!requestId.isEmpty()) {
      tunnelMessage.setRequestId(requestId);
    }
    stream.onNext(tunnelMessage.build());
  }
}
