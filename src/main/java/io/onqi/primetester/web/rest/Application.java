package io.onqi.primetester.web.rest;

import akka.actor.ActorSystem;
import akka.routing.RoundRobinPool;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.onqi.primetester.worker.WorkerActor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class Application extends ResourceConfig {

  private ActorSystem system;

  public Application() {

    system = ActorSystem.create("primetester");
    system.actorOf(WorkerActor.createProps().withRouter(new RoundRobinPool(5)), "workerActor");

    register(new AbstractBinder() {
      protected void configure() {
        bind(system).to(ActorSystem.class);
      }
    });

/*
    ObjectMapper om = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .setSerializationInclusion(NON_NULL)
            .setVisibility(defaultInstance()
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
*/

    //    register(new JacksonJsonProvider(om));
    register(new JacksonJsonProvider());
    register(LoggingFilter.class);

    packages(this.getClass().getPackage().getName());
  }

  @PreDestroy
  @SuppressWarnings("unused")
  private void shutdown() {
    system.shutdown();
    system.awaitTermination(Duration.create(15, TimeUnit.SECONDS));
  }
}
