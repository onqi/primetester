package io.onqi.primetester;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.annotation.PreDestroy;
import javax.ws.rs.ApplicationPath;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std.defaultInstance;

@ApplicationPath("/api")
public class Application extends ResourceConfig {
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
          .registerModule(new Jdk8Module())
          .configure(SerializationFeature.INDENT_OUTPUT, false)
          .configure(SerializationFeature.INDENT_OUTPUT, false)
          .setSerializationInclusion(NON_NULL)
          .setVisibility(defaultInstance()
                  .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                  .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

  private ActorSystemHolder systemHolder;

  public Application() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    systemHolder = new ActorSystemHolder();
    register(systemHolder.binder());

    register(new JacksonJsonProvider(OBJECT_MAPPER));

    register(new LoggingFilter());

    property(ServerProperties.TRACING, TracingConfig.OFF.toString());

    packages("io.onqi");
  }

  @PreDestroy
  @SuppressWarnings("unused")
  private void shutdown() {
    systemHolder.shutdown();
  }
}