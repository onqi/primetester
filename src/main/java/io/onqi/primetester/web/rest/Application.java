package io.onqi.primetester.web.rest;

import akka.actor.ActorSystem;
import akka.routing.RoundRobinPool;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.onqi.primetester.actors.RegistrarActor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import scala.concurrent.duration.Duration;

import javax.annotation.PreDestroy;
import javax.ws.rs.ApplicationPath;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std.defaultInstance;

@ApplicationPath("/")
public class Application extends ResourceConfig {

  private ActorSystem system;

  public Application() {

    system = ActorSystem.create("primetester");
    system.actorOf(RegistrarActor.createProps().withRouter(new RoundRobinPool(5)), "registrarRouter");

    register(new AbstractBinder() {
      protected void configure() {
        bind(system).to(ActorSystem.class);
      }
    });

    ObjectMapper om = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .setSerializationInclusion(NON_NULL)
            .setVisibility(defaultInstance()
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

    register(new JacksonJsonProvider(om));

    register(new LoggingFilter());
//    property(ServerProperties.TRACING, TracingConfig.ALL.toString());

    packages("io.onqi");
  }

  @PreDestroy
  @SuppressWarnings("unused")
  private void shutdown() {
    system.shutdown();
    system.awaitTermination(Duration.create(15, TimeUnit.SECONDS));
  }
}
