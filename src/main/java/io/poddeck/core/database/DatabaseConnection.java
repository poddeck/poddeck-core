package io.poddeck.core.database;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;

import java.util.Properties;

@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseConnection {
  public static DatabaseConnection create(DatabaseConfiguration configuration) {
    var connection = new DatabaseConnection(configuration);
    connection.configure();
    return connection;
  }

  private final DatabaseConfiguration configuration;
  @Getter
  private SessionFactory sessionFactory;

  public void configure() throws HibernateException {
    var hibernateConfiguration = new Configuration();
    hibernateConfiguration.setProperties(createHibernateSettings());
    var reflections = new Reflections("io.poddeck.core");
    for (var entity : reflections.getTypesAnnotatedWith(Entity.class)) {
      hibernateConfiguration.addAnnotatedClass(entity);
    }
    sessionFactory = hibernateConfiguration.buildSessionFactory();
  }

  private Properties createHibernateSettings() {
    var settings = new Properties();
    settings.put("hibernate.connection.driver_class", "org.postgresql.Driver");
    settings.put("hibernate.connection.url", String.format(
      "jdbc:postgresql://%s:%s/%s", configuration.hostname(),
      configuration.port(), configuration.database()));
    settings.put("hibernate.connection.username", configuration.username());
    settings.put("hibernate.connection.password", configuration.password());
    settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    settings.put("hibernate.show_sql", "false");
    settings.put("hibernate.format_sql", "false");
    settings.put("hibernate.hbm2ddl.auto", "update");
    settings.put("hibernate.connection.pool_size", "5");
    return settings;
  }

  public Session openSession() throws HibernateException {
    if (sessionFactory == null) {
      throw new HibernateException("Database not configured");
    }
    return sessionFactory.openSession();
  }

  public void shutdown() throws HibernateException {
    if (sessionFactory == null) {
      throw new HibernateException("Database not configured");
    }
    sessionFactory.close();
  }
}
