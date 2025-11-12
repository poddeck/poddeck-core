package io.poddeck.core.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.AbstractConfiguration;

@RequiredArgsConstructor(staticName = "create")
public final class DatabaseInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  DatabaseConfiguration provideDatabaseConfiguration(AbstractConfiguration file) {
    var configuration = DatabaseConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  @Provides
  @Singleton
  DatabaseConnection provideDatabaseConnection(
    DatabaseConfiguration configuration
  ) {
    return DatabaseConnection.create(configuration);
  }
}
