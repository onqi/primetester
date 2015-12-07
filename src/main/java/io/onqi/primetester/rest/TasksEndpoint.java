package io.onqi.primetester.rest;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.actors.NotificationRegistryActor;
import io.onqi.primetester.actors.TaskStorageActor;
import io.onqi.primetester.actors.TaskStorageActor.TaskIdAssignedMessage;
import io.onqi.primetester.actors.TaskStorageActor.TaskStatusMessage;
import io.onqi.primetester.rest.resources.CreateTaskResource;
import io.onqi.primetester.rest.resources.ErrorResource;
import io.onqi.primetester.rest.resources.TaskStatusResource;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.Broadcaster;
import org.glassfish.jersey.server.ManagedAsync;
import scala.concurrent.Future;

import java.net.URI;

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

import static akka.actor.ActorRef.noSender;
import static java.util.concurrent.TimeUnit.SECONDS;

@Path("/tasks")
public class TasksEndpoint {
  private static final Timeout TIMEOUT = Timeout.apply(2, SECONDS);

  @Context
  private ActorSystem actorSystem;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @SuppressWarnings("unchecked")
  public void createTask(@Valid CreateTaskResource resource, @Suspended final AsyncResponse response) {
    ActorRef taskStorage = actorSystem.actorFor(ActorSystemHolder.TASK_STORAGE_PATH);
    Future<Object> future = Patterns.ask(taskStorage, resource.toMessage(), TIMEOUT);
    future.onComplete(new CreateTaskCallback(response), actorSystem.dispatcher());
  }

  @GET
  @Path("{taskId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @SuppressWarnings({"VoidMethodAnnotatedWithGET", "unchecked"})
  public void getStatus(@PathParam("taskId") long taskId, @Suspended final AsyncResponse response) {
    ActorRef taskStorage = actorSystem.actorFor(ActorSystemHolder.TASK_STORAGE_PATH);
    Future<Object> future = Patterns.ask(taskStorage, new TaskStorageActor.GetTaskStatusMessage(taskId), TIMEOUT);
    future.onComplete(new GetStatusCallback(response), actorSystem.dispatcher());
  }

  @GET
  @Path("{taskId}/notifications")
  @Suspend(contentType = "application/json")
  public String suspend(@PathParam("taskId") Broadcaster topic) {
    ActorSelection notificationRegistry = actorSystem.actorSelection(ActorSystemHolder.NOTIFICATION_REGISTRY_PATH);
    notificationRegistry.tell(new NotificationRegistryActor.NotificationRegistration(topic), noSender());
    return "";
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
        TaskIdAssignedMessage message = (TaskIdAssignedMessage) result;
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
        TaskStatusMessage message = (TaskStatusMessage) result;
        if (TaskStatusMessage.NOT_FOUND.equals(result)) {
          res.resume(Response.status(Response.Status.NOT_FOUND).build());
        } else {
          if (TaskStorageActor.Status.FINISHED.equals(message.getStatus())) {
            URI resultLocation = UriBuilder.fromResource(ResultsEndpoint.class)
                    .path(ResultsEndpoint.class, "getResult")
                    .build(message.getNumber());

            res.resume(Response.seeOther(resultLocation).build());
          } else {
            res.resume(Response.ok(new TaskStatusResource(message.getTaskId(), message.getStatus())).build());
          }
        }
      }
    }
  }
}
