package io.onqi.primetester.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.rest.resources.TaskStatusResource;
import org.atmosphere.cpr.Broadcaster;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.onqi.primetester.actors.TaskStorageActor.Status.FINISHED;

public class NotificationRegistryActor extends UntypedActor {
  private final Map<String, Broadcaster> subscriptions = new HashMap<>();
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  public static Props createProps() {
    return Props.create(NotificationRegistryActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof NotificationRegistration) {
      Broadcaster b = ((NotificationRegistration) message).topic;
      subscriptions.put(b.getID(), b);

    } else if (message instanceof WorkerActor.CalculationStarted) {
      WorkerActor.CalculationStarted msg = (WorkerActor.CalculationStarted) message;
      long taskId = msg.getTaskId();
      Optional.ofNullable(subscriptions.get(String.valueOf(taskId))).ifPresent(b -> b.broadcast(new TaskStatusResource(taskId, FINISHED)));

    } else if (message instanceof WorkerActor.CalculationFinished) {
      WorkerActor.CalculationFinished msg = (WorkerActor.CalculationFinished) message;
      long taskId = msg.getTaskId();
      Optional.ofNullable(subscriptions.get(String.valueOf(taskId))).ifPresent(b -> b.broadcast(new TaskStatusResource(taskId, FINISHED)));

    } else {
      unhandled(message);
    }
  }

  public static class NotificationRegistration {
    private static final long serialVersionUID = 1L;

    private final Broadcaster topic;

    public NotificationRegistration(Broadcaster topic) {
      this.topic = topic;
    }
  }
}
