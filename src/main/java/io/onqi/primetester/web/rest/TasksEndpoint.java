package io.onqi.primetester.web.rest;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.onqi.primetester.actors.TaskManagerActor;
import io.onqi.primetester.actors.TaskManagerActor.TaskIdMessage;
import io.onqi.primetester.web.rest.resources.CreateTaskResource;
import io.onqi.primetester.web.rest.resources.StatusResource;
import org.glassfish.jersey.server.ManagedAsync;
import scala.concurrent.Future;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static java.util.concurrent.TimeUnit.SECONDS;

@Path("/tasks")
public class TasksEndpoint {
  @Context
  private ActorSystem actorSystem;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @SuppressWarnings("unchecked")
  public void createTask(@Valid CreateTaskResource resource, @Suspended final AsyncResponse response) {
    ActorRef taskManager = actorSystem.actorFor(ActorSystemHolder.TASK_MANAGER_PATH);
    Future<Object> future = Patterns.ask(taskManager, resource.toMessage(), Timeout.apply(2, SECONDS));
    future.onComplete(new CreateTaskCallback(response), actorSystem.dispatcher());
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @SuppressWarnings({"VoidMethodAnnotatedWithGET", "unchecked"})
  public void getStatus(@PathParam("id") long taskId, @Suspended final AsyncResponse response) {
    ActorSelection taskManager = actorSystem.actorSelection(ActorSystemHolder.TASK_MANAGER_PATH);
    Future<Object> future = Patterns.ask(taskManager, new TaskIdMessage(taskId), Timeout.apply(2, SECONDS));
    future.onComplete(new GetStatusCallback(response), actorSystem.dispatcher());
  }

  private class CreateTaskCallback extends OnComplete<Object> {
    private final AsyncResponse res;

    public CreateTaskCallback(AsyncResponse res) {
      this.res = res;
    }

    @Override
    public void onComplete(Throwable failure, Object result) throws Throwable {
      if (failure != null) {
        res.resume(new ErrorResource(failure.getMessage()).buildResponse());
      } else {
        TaskIdMessage message = (TaskIdMessage) result;
        URI statusLocation = UriBuilder.fromResource(TasksEndpoint.class)
                .path(TasksEndpoint.class, "getStatus")
                .build(message.getTaskId());

        res.resume(Response.created(statusLocation).build());
      }
    }
  }

  private class GetStatusCallback extends OnComplete<Object> {
    private final AsyncResponse res;

    public GetStatusCallback(AsyncResponse res) {
      this.res = res;
    }

    @Override
    public void onComplete(Throwable failure, Object result) throws Throwable {
      if (failure != null) {
        res.resume(new ErrorResource(failure.getMessage()).buildResponse());
      } else {
        TaskManagerActor.StatusResponse statusResponse = (TaskManagerActor.StatusResponse) result;
        if (TaskManagerActor.StatusResponse.NOT_FOUND.equals(result)) {
          res.resume(Response.status(Response.Status.NOT_FOUND).build());
        } else {
          res.resume(Response.ok(new StatusResource(statusResponse.getTaskId(), statusResponse.isFinished())).build());
        }
      }
    }
  }
}
