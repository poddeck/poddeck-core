package io.poddeck.core.api;

import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Configuration
public class ApiModule {
  @Bean
  ApiConfiguration apiConfiguration(AbstractConfiguration file) {
    var configuration = ApiConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  @Bean("authenticationKey")
  Key authenticationKey(@Qualifier("apiConfiguration") ApiConfiguration configuration) {
    return new SecretKeySpec(configuration.authenticationKey()
      .getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
  }

  @Bean("refreshKey")
  Key refreshKey(@Qualifier("apiConfiguration") ApiConfiguration configuration) {
    return new SecretKeySpec(configuration.refreshKey()
      .getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
  }
}
