package io.poddeck.core.api;

import io.poddeck.common.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.AbstractConfiguration;

import java.util.List;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class ApiConfiguration implements Configuration {
  private int port;
  private String panelKey;
  private List<String> allowedOrigins;

  @Override
  public void load(AbstractConfiguration file) {
    port = file.getInt("api.port");
    panelKey = file.getString("api.panel_key");
    allowedOrigins = file.getList(String.class, "api.allowed_origins");
  }
}