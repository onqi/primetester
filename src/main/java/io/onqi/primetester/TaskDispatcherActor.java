package io.onqi.primetester;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class TaskDispatcherActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  public static Props createProps() {
    return Props.create(StorageActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    unhandled(message);
  }

  @Override
  public void preStart() throws Exception {
    log.debug("Starting TaskDispatcher");
  }

  @Override
  public void postStop() throws Exception {
    log.debug("TaskDispatcher stopped");
  }
}
