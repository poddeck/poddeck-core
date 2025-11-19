package io.poddeck.core;

import io.poddeck.common.event.EventExecutor;
import io.poddeck.common.log.Log;
import io.poddeck.core.api.ApiConfiguration;
import io.poddeck.core.application.ApplicationLaunchEvent;
import io.poddeck.core.application.ApplicationPostRunEvent;
import io.poddeck.core.application.ApplicationPreRunEvent;
import io.poddeck.core.communication.CommunicationServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collections;

@SpringBootApplication(scanBasePackages = {"io.poddeck.core"})
public class CoreApplication {
  /**
   * The starting point where the application is executed
   *
   * @param args The arguments that are passed into the application
   */
  public static void main(String[] args) {
    try (var coreContext = new AnnotationConfigApplicationContext(CoreModule.class)) {
      var log = coreContext.getBean(Log.class);
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
        log.processError(throwable));
      try {
        log.info("Initializing PodDeck - Core");
        var eventExecutor = coreContext.getBean(EventExecutor.class);
        eventExecutor.execute(ApplicationLaunchEvent.create());
        var application = new SpringApplication(CoreApplication.class);
        var apiConfiguration = coreContext.getBean(ApiConfiguration.class);
        application.setDefaultProperties(Collections.singletonMap("server.port",
          apiConfiguration.port()));
        eventExecutor.execute(ApplicationPreRunEvent.create());
        log.info("Booting Spring...");
        var applicationContext = application.run(args);
        log.info("Spring successfully booted");
        log.info("Starting communication server...");
        var communicationServer = applicationContext.getBean(CommunicationServer.class);
        communicationServer.start();
        log.info("Communication server successfully started");
        log.info("Successfully booted PodDeck - Core");
        eventExecutor.execute(ApplicationPostRunEvent.create());
      } catch (Exception exception) {
        log.processError(exception);
      }
    }
  }
}