package io.poddeck.core.session;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Optional;

@Configuration
public class SessionModule {
  @Bean
  Optional<DatabaseReader> geoDatabaseReader() {
    var database = new File("geo/GeoLite2-City.mmdb");
    if (!database.exists()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new DatabaseReader.Builder(database).build());
    } catch (Exception exception) {
      return Optional.empty();
    }
  }
}