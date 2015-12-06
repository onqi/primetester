package io.onqi.primetester.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Storage for the prime test results.
 * Responsibilities: <ol>
 * <li>storing statuses</li>
 * <li>storing results</li>
 * </ol>
 */
public class ResultStorageActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  private HashMap<String, WorkerActor.CalculationFinished> results = new HashMap<>();

  public static Props createProps() {
    return Props.create(ResultStorageActor.class);
  }

  /**
   * {@link WorkerActor.CalculationFinished} from {@link WorkerActor} to persist calculation result<br/>
   */
  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof WorkerActor.CalculationFinished) {
      WorkerActor.CalculationFinished calculationFinished = (WorkerActor.CalculationFinished) message;

      results.put(calculationFinished.getNumber(), calculationFinished);

    } else if (message instanceof GetCalculationResultMessage) {
      String number = ((GetCalculationResultMessage) message).number;

      CalculationResultMessage result = Optional.ofNullable(results.get(number))
              .map(cf -> new CalculationResultMessage(cf.getNumber(), cf.isPrime(), cf.getDivider()))
              .orElse(CalculationResultMessage.NOT_FOUND);
      getSender().tell(result, self());

    } else {
      unhandled(message);
    }
  }

  @Override
  public void postStop() throws Exception {
    log.debug("Storage stopped");
  }


  HashMap<String, WorkerActor.CalculationFinished> getResults() {
    return results;
  }

  public static class GetCalculationResultMessage {
    private static final long serialVersionUID = 1L;

    private final String number;

    public GetCalculationResultMessage(String number) {
      this.number = number;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      GetCalculationResultMessage that = (GetCalculationResultMessage) o;
      return Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
      return Objects.hash(number);
    }

    @Override
    public String toString() {
      return "GetCalculationResultMessage{" +
              "number='" + number + '\'' +
              '}';
    }
  }

  public static class CalculationResultMessage {
    private static final long serialVersionUID = 1L;
    public static final CalculationResultMessage NOT_FOUND = new CalculationResultMessage("", false, Optional.empty());

    private final String number;
    private final boolean isPrime;
    private final Optional<String> divider;

    public CalculationResultMessage(String number, boolean isPrime, Optional<String> divider) {
      this.number = number;
      this.isPrime = isPrime;
      this.divider = divider;
    }

    public String getNumber() {
      return number;
    }

    public boolean isPrime() {
      return isPrime;
    }

    public Optional<String> getDivider() {
      return divider;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CalculationResultMessage that = (CalculationResultMessage) o;
      return isPrime == that.isPrime &&
              Objects.equals(number, that.number) &&
              Objects.equals(divider, that.divider);
    }

    @Override
    public int hashCode() {
      return Objects.hash(number, isPrime, divider);
    }

    @Override
    public String toString() {
      return "CalculationResultMessage{" +
              "number='" + number + '\'' +
              ", isPrime=" + isPrime +
              ", divider=" + divider +
              '}';
    }
  }
}



