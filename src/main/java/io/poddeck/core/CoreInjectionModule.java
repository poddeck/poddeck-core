package io.poddeck.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.poddeck.common.configuration.ConfigurationInjectionModule;
import io.poddeck.common.log.Log;
import io.poddeck.core.database.DatabaseInjectionModule;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CoreInjectionModule extends AbstractModule {
  public static CoreInjectionModule create() {
    return new CoreInjectionModule();
  }

  @Override
  protected void configure() {
    install(ConfigurationInjectionModule.create());
    install(DatabaseInjectionModule.create());
  }

  @Provides
  @Singleton
  Log provideCoreLog() throws Exception {
    return Log.create("Core");
  }
}
