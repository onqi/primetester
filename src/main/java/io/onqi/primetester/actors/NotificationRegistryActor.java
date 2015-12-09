package io.onqi.primetester.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.rest.resources.TaskStatusResource;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;

import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.onqi.primetester.actors.TaskStorageActor.Status.FINISHED;
import static io.onqi.primetester.actors.TaskStorageActor.Status.STARTED;

public class NotificationRegistryActor extends UntypedActor {
  public static final String TOPIC = "statusMessages";

  private final LoggingAdapter log = Logging.getLogger(context().system(), this);
  private final Map<Long, SseBroadcaster> subscriptions = new HashMap<>();

  public static Props createProps() {
    return Props.create(NotificationRegistryActor.class);
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting NotificationRegistryActor");
    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
    mediator.tell(new DistributedPubSubMediator.Subscribe(NotificationRegistryActor.TOPIC, getSelf()), getSelf());
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.info("Received message {}", message);
    if (message instanceof NotificationRegistration) {
      registerNewSSESubscription((NotificationRegistration) message);

    } else if (message instanceof WorkerActor.CalculationStarted) {
      broadcast(((WorkerActor.CalculationStarted) message).getTaskId(), STARTED);

    } else if (message instanceof WorkerActor.CalculationFinished) {
      broadcast(((WorkerActor.CalculationFinished) message).getTaskId(), FINISHED);

    } else if (message instanceof DistributedPubSubMediator.SubscribeAck) {
      logSubscribeAck();

    } else {
      unhandled(message);
    }
  }

  private void registerNewSSESubscription(NotificationRegistration message) {
    subscriptions.put(message.taskId, message.broadcaster);
  }


  private void broadcast(long taskId, TaskStorageActor.Status status) {
    OutboundEvent event = new OutboundEvent.Builder()
            .id(String.valueOf(taskId))
            .name(status.toString())
            .data(TaskStatusResource.class, new TaskStatusResource(taskId, null, status))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
    Optional.ofNullable(subscriptions.get(taskId)).ifPresent(b -> b.broadcast(event));
  }

  private void logSubscribeAck() {
    log.info("Successfully subscribed for topic '{}'", NotificationRegistryActor.TOPIC);
  }

  public static class NotificationRegistration implements Serializable {
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
