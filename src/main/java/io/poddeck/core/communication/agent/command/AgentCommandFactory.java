package io.poddeck.core.communication.agent.command;

import io.poddeck.common.log.Log;
import io.poddeck.core.communication.agent.Agent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgentCommandFactory {
  private final Log log;
  private final AgentCommandRegistry agentCommandRegistry;

  public AgentCommand create(Agent agent) {
    return AgentCommand.create(log, agentCommandRegistry, agent);
  }
}
