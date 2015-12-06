package io.onqi.primetester;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.WorkerActor.CalculationResult;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static akka.actor.ActorRef.noSender;

/**
 * Storage for the prime test results.
 * Responsibilities: <ol>
 * <li>storing results</li>
 * </ol>
 */
public class StorageActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  private AtomicLong id = new AtomicLong();
  private HashMap<Long, Status> statuses = new HashMap<>();
  private HashMap<String, CalculationResult> results = new HashMap<>();

  public static Props createProps() {
    return Props.create(StorageActor.class);
  }

  /**
   * {@link CalculationResult} from {@link io.onqi.primetester.WorkerActor} to persist calculation result<br/>
   */
  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof NewNumberCalculationMessage) {
      NewNumberCalculationMessage msg = (NewNumberCalculationMessage) message;
      long taskId = id.incrementAndGet();
      statuses.put(taskId, Status.QUEUED);

      getSender().tell(new TaskIdAssignedMessage(taskId, msg.getNumber()), noSender());

    } else if (message instanceof WorkerActor.CalculationResult) {
      CalculationResult calculationResult = (CalculationResult) message;

      results.put(calculationResult.getNumber(), calculationResult);
      statuses.put(calculationResult.getTaskId(), Status.FINISHED);

    } else {
      unhandled(message);
    }
  }

  @Override
  public void postStop() throws Exception {
    log.debug("Storage stopped");
  }

  HashMap<Long, Status> getStatuses() {
    return statuses;
  }

  HashMap<String, CalculationResult> getResults() {
    return results;
  }

  public enum Status {
    QUEUED,
    STARTED,
    FINISHED
  }
}



