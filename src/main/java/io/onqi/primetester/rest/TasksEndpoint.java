package io.onqi.primetester.rest;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.actors.TaskStorageActor;
import io.onqi.primetester.actors.TaskStorageActor.TaskIdAssignedMessage;
import io.onqi.primetester.actors.TaskStorageActor.TaskStatusMessage;
import io.onqi.primetester.rest.resources.CreateTaskResource;
import io.onqi.primetester.rest.resources.ErrorResource;
import io.onqi.primetester.rest.resources.TaskStatusResource;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.BroadcasterListener;
import org.glassfish.jersey.server.ChunkedOutput;
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

import static io.onqi.primetester.actors.TaskStorageActor.Status.QUEUED;
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
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput suspend(@PathParam("taskId") long taskId) {
    ActorRef taskStorage = actorSystem.actorFor(ActorSystemHolder.TASK_STORAGE_PATH);
    SseBroadcaster broadcaster = createNewBroadcaster();
    final EventOutput eventOutput = new EventOutput();

    if (!broadcaster.add(eventOutput)) {
      actorSystem.log().error("Unable to add Event Output to a broadcaster!!!");
    }
    taskStorage.tell(new TaskStorageActor.NotificationRegistration(taskId, broadcaster), ActorRef.noSender());
    return eventOutput;
  }

  private SseBroadcaster createNewBroadcaster() {
    SseBroadcaster broadcaster = new SseBroadcaster();
    broadcaster.add(new BroadcasterListener<OutboundEvent>() {
      @Override
      public void onException(ChunkedOutput<OutboundEvent> chunkedOutput, Exception exception) {

        actorSystem.log().error("An exception has been thrown while broadcasting to an event output.", exception);
      }

      @Override
      public void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
        actorSystem.log().debug("Connection has been closed");
      }
    });

    return broadcaster;
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

        res.resume(Response.ok(new TaskStatusResource(message.getTaskId(), message.getNumber(), QUEUED)).build());
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
            URI resultLocation = UriBuilder.fromPath("/api").path(ResultsEndpoint.class)
                    .path(ResultsEndpoint.class, "getResult")
                    .build(message.getNumber());

            res.resume(Response.seeOther(resultLocation).build());
          } else {
            res.resume(Response.ok(new TaskStatusResource(message.getTaskId(), message.getNumber(), message.getStatus())).build());
          }
        }
      }
    }
  }
}
