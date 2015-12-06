package io.onqi.primetester;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.WorkerActor.CalculationResult;

import java.util.HashMap;

/**
 * Storage for the prime test results.
 * Responsibilities: <ol>
 * <li>storing results</li>
 * </ol>
 */
public class StorageActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

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
    if (message instanceof CalculationResult) {
      CalculationResult calculationResult = (CalculationResult) message;

      results.put(calculationResult.getNumber(), calculationResult);
      statuses.put(calculationResult.getId(), Status.FINISHED);
    } else {
      unhandled(message);
    }
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



