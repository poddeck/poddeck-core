package io.poddeck.core;

import io.poddeck.common.event.EventExecutor;
import io.poddeck.common.event.HookRegistry;
import io.poddeck.common.log.Log;
import io.poddeck.core.api.ApiConfiguration;
import io.poddeck.core.application.ApplicationLaunchEvent;
import io.poddeck.core.communication.CommunicationHook;
import io.poddeck.core.communication.CommunicationServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
        var application = new SpringApplication(CoreApplication.class);
        var apiConfiguration = coreContext.getBean(ApiConfiguration.class);
        application.setDefaultProperties(Collections.singletonMap("server.port",
          apiConfiguration.port()));
        log.info("Booting Spring...");
        var applicationContext = application.run(args);
        var eventExecutor = applicationContext.getBean(EventExecutor.class);
        registerHooks(applicationContext);
        log.info("Spring successfully booted");
        log.info("Starting communication server...");
        var communicationServer = applicationContext.getBean(CommunicationServer.class);
        communicationServer.start();
        log.info("Communication server successfully started");
        log.info("Successfully booted PodDeck - Core");
        eventExecutor.execute(ApplicationLaunchEvent.create());
      } catch (Exception exception) {
        log.processError(exception);
      }
    }
  }

  private static void registerHooks(ConfigurableApplicationContext context) {
    var hookRegistry = context.getBean(HookRegistry.class);
    hookRegistry.register(context.getBean(CommunicationHook.class));
  }
}