package io.poddeck.core.communication;

import io.poddeck.common.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.AbstractConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class CommunicationConfiguration implements Configuration {
  private int port;

  @Override
  public void load(AbstractConfiguration file) {
    port = file.getInt("communication.port");
  }
}
