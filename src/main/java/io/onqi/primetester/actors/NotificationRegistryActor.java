package io.onqi.primetester.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.rest.resources.TaskStatusResource;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.onqi.primetester.actors.TaskStorageActor.Status.FINISHED;
import static io.onqi.primetester.actors.TaskStorageActor.Status.STARTED;

public class NotificationRegistryActor extends UntypedActor {
  private final Map<Long, SseBroadcaster> subscriptions = new HashMap<>();
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  public static Props createProps() {
    return Props.create(NotificationRegistryActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof NotificationRegistration) {
      NotificationRegistration msg = (NotificationRegistration) message;
      subscriptions.put(msg.taskId, msg.broadcaster);

    } else if (message instanceof WorkerActor.CalculationStarted) {
      WorkerActor.CalculationStarted msg = (WorkerActor.CalculationStarted) message;

      long taskId = msg.getTaskId();
      OutboundEvent event = new OutboundEvent.Builder()
              .id(String.valueOf(taskId))
              .name(STARTED.toString())
              .data(TaskStatusResource.class, new TaskStatusResource(taskId, null, STARTED))
              .mediaType(MediaType.APPLICATION_JSON_TYPE)
              .build();
      Optional.ofNullable(subscriptions.get(taskId)).ifPresent(b -> b.broadcast(event));

    } else if (message instanceof WorkerActor.CalculationFinished) {
      WorkerActor.CalculationFinished msg = (WorkerActor.CalculationFinished) message;

      long taskId = msg.getTaskId();
      OutboundEvent event = new OutboundEvent.Builder()
              .id(String.valueOf(taskId))
              .name(FINISHED.toString())
              .data(TaskStatusResource.class, new TaskStatusResource(taskId, null, FINISHED))
              .mediaType(MediaType.APPLICATION_JSON_TYPE)
              .build();
      Optional.ofNullable(subscriptions.get(taskId)).ifPresent(b -> b.broadcast(event));

    } else {
      unhandled(message);
    }
  }

  public static class NotificationRegistration {
    private static final long serialVersionUID = 1L;

    private final long taskId;
    private final SseBroadcaster broadcaster;

    public NotificationRegistration(long taskId, SseBroadcaster broadcaster) {
      this.taskId = taskId;
      this.broadcaster = broadcaster;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      NotificationRegistration that = (NotificationRegistration) o;
      return taskId == that.taskId &&
              Objects.equals(broadcaster, that.broadcaster);
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId, broadcaster);
    }

    @Override
    public String toString() {
      return "NotificationRegistration{" +
              "taskId=" + taskId +
              ", broadcaster=" + broadcaster +
              '}';
    }
  }
}
