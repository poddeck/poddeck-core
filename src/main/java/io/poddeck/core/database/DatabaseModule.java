package io.poddeck.core.database;

import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseModule {
  @Bean
  public DatabaseConfiguration databaseConfiguration(AbstractConfiguration file) {
    var configuration = DatabaseConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  @Bean
  public DataSource dataSource(DatabaseConfiguration configuration) {
    return DataSourceBuilder.create()
      .driverClassName("org.postgresql.Driver")
      .url(String.format("jdbc:postgresql://%s:%s/%s", configuration.hostname(),
        configuration.port(), configuration.database()))
      .username(configuration.username())
      .password(configuration.password())
      .build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
    DataSource dataSource
  ) {
    var entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
    entityManagerFactory.setDataSource(dataSource);
    entityManagerFactory.setPackagesToScan("io.poddeck.core");
    entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    var properties = new Properties();
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.hbm2ddl.auto", "update");
    properties.put("hibernate.show_sql", "false");
    entityManagerFactory.setJpaProperties(properties);
    return entityManagerFactory;
  }

  @Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }
}
