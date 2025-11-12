package io.poddeck.core.communication;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.AbstractConfiguration;

@RequiredArgsConstructor(staticName = "create")
public final class CommunicationInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  CommunicationConfiguration provideCommunicationConfiguration(
    AbstractConfiguration file
  ) {
    var configuration = CommunicationConfiguration.create();
    configuration.load(file);
    return configuration;
  }
}
