package io.onqi.primetester;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.perf4j.slf4j.Slf4JStopWatch;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import static akka.actor.ActorRef.noSender;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class WorkerActor extends UntypedActor {
  private static final BigInteger TWO = BigInteger.valueOf(2L);
  private static final BigInteger THREE = BigInteger.valueOf(3L);
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public static Props createProps() {
    return Props.create(WorkerActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof TaskIdAssignedMessage) {
      CalculationResult response = checkIsPrime((TaskIdAssignedMessage) message);
      getSender().tell(response, noSender());
    } else {
      unhandled(message);
    }
  }

  @Override
  public void postStop() throws Exception {
    log.debug("Worker stopped");
  }

  /**
   * Calculation doesn't utilize the power of {@link BigInteger#isProbablePrime(int)} on purpose as we need the processing to take longer than 20ms
   */
  public CalculationResult checkIsPrime(TaskIdAssignedMessage message) {
    BigInteger n = new BigInteger(message.getNumber());
    log.debug("Checking {}", n);
    Slf4JStopWatch stopWatch = new Slf4JStopWatch("worker");
    try {
      if (ZERO.equals(n) || ONE.equals(n)) {
        return new CalculationResult(message.getTaskId(), message.getNumber(), true, Optional.empty());
      }

      if (TWO.equals(n)) {
        return new CalculationResult(message.getTaskId(), message.getNumber(), false, Optional.of(ONE.toString()));
      }

      BigInteger root = approximateRoot(n);
      log.debug("{}: Using approximate root {}", n, root);

      for (BigInteger divider = THREE; divider.compareTo(root) <= 0; divider = divider.nextProbablePrime()) {
        if (n.mod(divider).equals(ZERO)) {
          log.debug("{}: divides by {}", n, divider);
          return new CalculationResult(message.getTaskId(), message.getNumber(), false, Optional.of(divider.toString()));
        }
      }
      return new CalculationResult(message.getTaskId(), message.getNumber(), true, Optional.empty());
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

  public static class CalculationResult {
    private static final long serialVersionUID = 1L;

    private final long taskId;
    private final String number;
    private final boolean isPrime;
    private final Optional<String> divider;

    public CalculationResult(long taskId, String number, boolean isPrime, Optional<String> divider) {
      this.taskId = taskId;
      this.number = number;
      this.isPrime = isPrime;
      this.divider = divider;
    }

    public long getTaskId() {
      return taskId;
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
      CalculationResult that = (CalculationResult) o;
      return taskId == that.taskId &&
              isPrime == that.isPrime &&
              Objects.equals(number, that.number) &&
              Objects.equals(divider, that.divider);
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId, number, isPrime, divider);
    }

    @Override
    public String toString() {
      return "CalculationResult{" +
              "taskId=" + taskId +
              ", number='" + number + '\'' +
              ", isPrime=" + isPrime +
              ", divider=" + divider +
              '}';
    }
  }
}