package io.onqi.primetester.rest;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.actors.ResultStorage.CalculationResultMessage;
import io.onqi.primetester.actors.ResultStorage.GetCalculationResultMessage;
import io.onqi.primetester.rest.resources.CreateTaskResource;
import io.onqi.primetester.rest.resources.ErrorResource;
import io.onqi.primetester.rest.resources.ResultResource;
import org.glassfish.jersey.server.ManagedAsync;
import scala.concurrent.Future;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.util.concurrent.TimeUnit.SECONDS;

@Path("/results")
public class ResultsEndpoint {
  private static final Timeout TIMEOUT = Timeout.apply(2, SECONDS);

  @Context
  private ActorSystem actorSystem;

  @GET
  @Path("{number}")
  @Produces(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @SuppressWarnings({"VoidMethodAnnotatedWithGET", "unchecked"})
  public void getResult(@PathParam("number") @NotNull @Pattern(regexp = CreateTaskResource.NUMBER_PATTERN) String number,
                        @Suspended final AsyncResponse response) {
    ActorSelection resultStorage = actorSystem.actorSelection(ActorSystemHolder.RESULT_STORAGE_PATH);
    Future<Object> future = Patterns.ask(resultStorage, new GetCalculationResultMessage(number), TIMEOUT);
    future.onComplete(new ResultCallback(response), actorSystem.dispatcher());
  }

  private class ResultCallback extends OnComplete<Object> {
    private final AsyncResponse res;

    public ResultCallback(AsyncResponse res) {
      this.res = res;
    }

    @Override
    public void onComplete(Throwable failure, Object result) throws Throwable {
      if (failure != null) {
        res.resume(new ErrorResource(failure.getMessage()).buildResponse());
      } else {
        CalculationResultMessage response = (CalculationResultMessage) result;
        if (CalculationResultMessage.NOT_FOUND.equals(result)) {
          res.resume(Response.status(Response.Status.NOT_FOUND).build());
        } else {
          ResultResource resource = new ResultResource(response.getNumber(), response.isPrime(), response.getDivider());
          res.resume(Response.ok(resource).build());
        }
      }
    }
  }
}
