package io.onqi.primetester.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.perf4j.slf4j.Slf4JStopWatch;

import java.math.BigInteger;
import java.util.Optional;

import static akka.actor.ActorRef.noSender;
import static io.onqi.primetester.actors.WorkerActor.CalculationResponse.notPrime;
import static io.onqi.primetester.actors.WorkerActor.CalculationResponse.prime;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Optional.empty;

public class WorkerActor extends UntypedActor {
  private static final BigInteger THREE = BigInteger.valueOf(3L);
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public static Props createProps() {
    return Props.create(WorkerActor.class);
  }

  /**
   * {@link CalculationRequest} from {@link TaskManagerActor} to start the calculation
   */
  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof CalculationRequest) {
      CalculationResponse response = checkIsPrime((CalculationRequest) message);
      getSender().tell(response, noSender());
    } else {
      unhandled(message);
    }
  }

  /**
   * Calculation doesn't utilize the power of {@link BigInteger#isProbablePrime(int)} on purpose as we need the processing to take longer than 20ms
   */
  public CalculationResponse checkIsPrime(CalculationRequest request) {
    BigInteger n = new BigInteger(request.getNumber());
    log.debug("Checking {}", n);
    Slf4JStopWatch stopWatch = new Slf4JStopWatch("worker");
    try {
      if (n.equals(ONE) || n.equals(ZERO)) {
        return prime(request.id, request.number);
      }

      BigInteger root = approximateRoot(n);
      log.debug("{}: Using approximate root {}", n, root);

      for (BigInteger divider = THREE; divider.compareTo(root) <= 0; divider = divider.nextProbablePrime()) {
        if (n.mod(divider).equals(ZERO)) {
          log.debug("{}: divides by {}", n, divider);
          return notPrime(request.id, request.number, divider.toString());
        }
      }
      return prime(request.id, request.number);
    } finally {
      stopWatch.stop();
    }
  }

  private BigInteger approximateRoot(BigInteger n) {
    BigInteger half = n.shiftRight(1);
    while (half.multiply(half).compareTo(n) > 0) {
      half = half.shiftRight(1);
    }
    return half.shiftLeft(1);
  }

  public static class CalculationRequest {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String number;

    public CalculationRequest(long id, String number) {
      this.id = id;
      this.number = number;
    }

    public long getId() {
      return id;
    }

    public String getNumber() {
      return number;
    }
  }

  public static class CalculationResponse extends CalculationRequest {
    private static final long serialVersionUID = 1L;
    public static final CalculationResponse NOT_FOUND = new CalculationResponse(Long.MIN_VALUE, "", false, empty());

    private final boolean isPrime;
    private final Optional<String> divider;

    private CalculationResponse(long id, String number, boolean isPrime, Optional<String> divider) {
      super(id, number);
      this.isPrime = isPrime;
      this.divider = divider;
    }

    public boolean isPrime() {
      return isPrime;
    }

    public Optional<String> getDivider() {
      return divider;
    }

    static CalculationResponse prime(long taskId, String number) {
      return new CalculationResponse(taskId, number, true, Optional.empty());
    }

    static CalculationResponse notPrime(long taskId, String number, String divider) {
      return new CalculationResponse(taskId, number, true, Optional.of(divider));
    }
  }
}
