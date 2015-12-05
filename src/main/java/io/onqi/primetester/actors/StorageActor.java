package io.onqi.primetester.actors;

import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.actors.WorkerActor.CalculationResponse;
import io.onqi.primetester.web.rest.ActorSystemHolder;

import java.util.HashMap;
import java.util.Optional;

import static akka.actor.ActorRef.noSender;
import static io.onqi.primetester.actors.WorkerActor.CalculationResponse.NOT_FOUND;

/**
 * Storage for the prime test results.
 * Responsibilities: <ol>
 * <li>storing results</li>
 * <li>telling {@link NotifierActor} the result of new prime test</li>
 * </ol>
 */
public class StorageActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);
  private ActorSelection notifierRouter;

  private HashMap<String, CalculationResponse> results = new HashMap<>();

  public static Props createProps() {
    return Props.create(StorageActor.class);
  }

  /**
   * {@link CalculationResponse} from {@link TaskManagerActor} to persist calculation result<br/>
   * {@link ResultRequest} to query test result, {@link CalculationResponse#NOT_FOUND} if does not exist
   */
  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    /* no-res from TaskManagerActor.onMessage */
    if (message instanceof CalculationResponse) {
      CalculationResponse calculationResponse = (CalculationResponse) message;

      results.put(calculationResponse.getNumber(), calculationResponse);
      notifierRouter.tell(calculationResponse, noSender());

      /* res from ResultsEndpoint */
    } else if (message instanceof ResultRequest) {
      ResultRequest resultRequest = (ResultRequest) message;
      CalculationResponse response = Optional.ofNullable(results.get(resultRequest.getNumber())).orElse(NOT_FOUND);
      getSender().tell(response, noSender());

    } else {
      unhandled(message);
    }
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting storage");
    notifierRouter = context().system().actorSelection(ActorSystemHolder.NOTIFIER_ROUTER_PATH);
  }

  @Override
  public void postStop() throws Exception {
    log.info("Storage stopped");
    notifierRouter.tell(PoisonPill.getInstance(), noSender());
  }

  public static class ResultRequest {
    private static final long serialVersionUID = 1L;

    private final String number;

    public ResultRequest(String number) {
      this.number = number;
    }

    public String getNumber() {
      return number;
    }
  }
}



