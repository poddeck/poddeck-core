package io.poddeck.core;

import io.poddeck.common.event.EventExecutor;
import io.poddeck.common.event.HookRegistry;
import io.poddeck.common.log.Log;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class CoreModule {
  @Bean
  Log coreLog() throws Exception {
    return Log.create("Core");
  }

  @Bean
  AbstractConfiguration configurationFile() throws Exception {
    return new Configurations().ini(new File("config.ini"));
  }

  @Bean
  HookRegistry hookRegistry() {
    return HookRegistry.create();
  }

  @Bean
  EventExecutor eventExecutor(HookRegistry registry, Log log) {
    return EventExecutor.create(registry, log);
  }
}
