package io.onqi.primetester.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.FromConfig;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class ClusterProxy extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  private final ActorRef workerRouter = getContext().actorOf(FromConfig.getInstance().props(Worker.createProps()), "workerRouter");

  public static Props createProps() {
    return Props.create(ClusterProxy.class);
  }

  @Override
  public PartialFunction<Object, BoxedUnit> receive() {
    return ReceiveBuilder
            .match(TaskStorage.TaskIdAssignedMessage.class, this::forward)
            .matchAny(this::unhandled)
            .build();
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting Cluster Proxy");
  }

  private void forward(TaskStorage.TaskIdAssignedMessage message) {
    log.info("Forwarding message {} to the routees", message);
    workerRouter.tell(message, sender());
  }
}
