package io.onqi.primetester.actors;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.ActorSystemHolder;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import static akka.actor.ActorRef.noSender;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class WorkerActor extends UntypedActor {
  private static final BigInteger TWO = BigInteger.valueOf(2L);
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  private ActorSelection taskStorage;
  private ActorSelection resultStorage;

  public static Props createProps() {
    return Props.create(WorkerActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof TaskStorageActor.TaskIdAssignedMessage) {
      taskStorage.tell(new CalculationStarted(((TaskStorageActor.TaskIdAssignedMessage) message).getTaskId()), noSender());
      CalculationFinished response = checkIsPrime((TaskStorageActor.TaskIdAssignedMessage) message);
      resultStorage.tell(response, noSender());
      taskStorage.tell(response, noSender());
    } else {
      unhandled(message);
    }
  }

  @Override
  public void preStart() throws Exception {
    taskStorage = context().system().actorSelection(ActorSystemHolder.TASK_STORAGE_PATH);
    resultStorage = context().system().actorSelection(ActorSystemHolder.RESULT_STORAGE_PATH);
  }

  @Override
  public void postStop() throws Exception {
    log.debug("Worker stopped");
  }

  /**
   * Calculation doesn't utilize the power of {@link BigInteger#isProbablePrime(int)} on purpose as we need the processing to take longer than 20ms
   */
  public CalculationFinished checkIsPrime(TaskStorageActor.TaskIdAssignedMessage message) {
    BigInteger n = new BigInteger(message.getNumber());
    log.debug("Checking {}", n);
    if (ZERO.equals(n) || ONE.equals(n) || TWO.equals(n)) {
      return new CalculationFinished(message.getTaskId(), message.getNumber(), true, Optional.empty());
    }

    BigInteger root = approximateRoot(n);
    log.debug("{}: Using approximate root {}", n, root);

    for (BigInteger divider = TWO; divider.compareTo(root) <= 0; divider = divider.nextProbablePrime()) {
      if (n.mod(divider).equals(ZERO)) {
        log.debug("{}: divides by {}", n, divider);
        return new CalculationFinished(message.getTaskId(), message.getNumber(), false, Optional.of(divider.toString()));
      }
    }
    return new CalculationFinished(message.getTaskId(), message.getNumber(), true, Optional.empty());
  }

  private BigInteger approximateRoot(BigInteger n) {
    BigInteger half = n.shiftRight(1);
    while (half.multiply(half).compareTo(n) > 0) {
      half = half.shiftRight(1);
    }
    return half.shiftLeft(1);
  }

  public static class CalculationStarted {
    private long taskId;

    public CalculationStarted(long taskId) {
      this.taskId = taskId;
    }

    public long getTaskId() {
      return taskId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CalculationStarted that = (CalculationStarted) o;
      return taskId == that.taskId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId);
    }

    @Override
    public String toString() {
      return "CalculationStarted{" +
              "taskId=" + taskId +
              '}';
    }
  }

  public static class CalculationFinished {
    private static final long serialVersionUID = 1L;

    private final long taskId;
    private final String number;
    private final boolean isPrime;
    private final Optional<String> divider;

    public CalculationFinished(long taskId, String number, boolean isPrime, Optional<String> divider) {
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
      CalculationFinished that = (CalculationFinished) o;
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
      return "CalculationFinished{" +
              "taskId=" + taskId +
              ", number='" + number + '\'' +
              ", isPrime=" + isPrime +
              ", divider=" + divider +
              '}';
    }
  }
}