package io.poddeck.core.communication;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunicationModule {
  @Bean
  CommunicationConfiguration communicationConfiguration(
    AbstractConfiguration file
  ) {
    var configuration = CommunicationConfiguration.create();
    configuration.load(file);
    return configuration;
  }
}
