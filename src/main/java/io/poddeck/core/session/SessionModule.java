package io.poddeck.core.session;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class SessionModule {
  @Bean
  DatabaseReader geoDatabaseReader() throws Exception {
    var database = new File("geo/GeoLite2-City.mmdb");
    return new DatabaseReader.Builder(database).build();
  }
}