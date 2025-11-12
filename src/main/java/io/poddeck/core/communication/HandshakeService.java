package io.poddeck.core.communication;

import io.grpc.stub.StreamObserver;
import io.poddeck.common.HandshakeRequest;
import io.poddeck.common.HandshakeResponse;
import io.poddeck.common.HandshakeServiceGrpc;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public class HandshakeService extends HandshakeServiceGrpc.HandshakeServiceImplBase {
  @Override
  public void handshake(
    HandshakeRequest request, StreamObserver<HandshakeResponse> responseObserver
  ) {
    //TODO: IMPLEMENT
  }
}