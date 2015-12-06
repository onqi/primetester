package io.onqi.primetester.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.NewNumberCalculationMessage;

public class TaskDispatcherActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  private ActorSelection worker;
  private ActorSelection storage;

  public static Props createProps() {
    return Props.create(TaskDispatcherActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof NewNumberCalculationMessage) {
      storage.tell(message, getSelf());
    } else if (message instanceof StorageActor.TaskIdAssignedMessage) {
      worker.tell(message, getSelf());
    } else if (message instanceof WorkerActor.CalculationFinished) {
      storage.tell(message, ActorRef.noSender());
    } else {
      unhandled(message);
    }
  }

  @Override
  public void preStart() throws Exception {
    log.debug("Starting TaskDispatcher");
    worker = context().system().actorSelection(ActorSystemHolder.WORKER_PATH);
    storage = context().system().actorSelection(ActorSystemHolder.STORAGE_PATH);
  }

  @Override
  public void postStop() throws Exception {
    log.debug("TaskDispatcher stopped");
  }
}
