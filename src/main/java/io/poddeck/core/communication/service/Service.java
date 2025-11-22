package io.poddeck.core.communication.service;

import com.google.protobuf.Message;
import io.poddeck.core.communication.agent.Agent;

public interface Service<T extends Message> {
  void process(Agent agent, T message);
}
