package io.onqi.primetester.web.rest;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.onqi.primetester.web.rest.resources.ErrorResource;
import io.onqi.primetester.web.rest.resources.PrimeTestResource;
import org.glassfish.jersey.server.ManagedAsync;
import scala.concurrent.Future;

import java.util.Optional;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static java.util.concurrent.TimeUnit.SECONDS;

@Path(PrimeTestEndpoint.LOCATION)
public class PrimeTestEndpoint {
  public static final String LOCATION = "/prime-test";
  @Context
  ActorSystem actorSystem;
  LoggingAdapter log;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @ManagedAsync
  public void postNumber(@Valid PrimeTestResource resource, @Suspended final AsyncResponse response) {
    ActorSelection registrar = actorSystem.actorSelection("/user/registrarRouter");
    Timeout timeout = Timeout.apply(2, SECONDS);
    Future<Object> future = Patterns.ask(registrar, resource, timeout);

    future.onComplete(new RegistrationCallback(response), actorSystem.dispatcher());
  }

  private class RegistrationCallback extends OnComplete<Object> {
    private final AsyncResponse res;

    public RegistrationCallback(AsyncResponse res) {
      this.res = res;
    }

    @Override
    public void onComplete(Throwable failure, Object registrationId) throws Throwable {
      if (failure != null) {
        res.resume(Response.serverError().entity(new ErrorResource(Optional.of(failure.getMessage()))).build());
      } else {
        res.resume(Response.created(UriBuilder.fromResource(PrimeTestEndpoint.class).path(PrimeTestEndpoint.class, "getStatus").build(registrationId)).build());
      }
    }
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @SuppressWarnings("VoidMethodAnnotatedWithGET")
  public void getStatus(@PathParam("id") Long taskId) {

  }
}
