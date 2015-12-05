package io.onqi.primetester.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.actors.WorkerActor.CalculationResponse;

public class NotifierActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  public static Props createProps() {
    return Props.create(NotifierActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof CalculationResponse) {
      CalculationResponse pushRequest = (CalculationResponse) message;
      notify(pushRequest);
    } else {
      unhandled(message);
    }
  }

  private void notify(CalculationResponse pushRequest) {

  }

  @Override
  public void postStop() throws Exception {
    log.info("Notifier stopped");
    super.postStop();
  }
}
