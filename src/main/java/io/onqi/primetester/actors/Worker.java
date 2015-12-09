package io.onqi.primetester.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class Worker extends AbstractActor {
  private static final BigInteger TWO = BigInteger.valueOf(2L);
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  private ActorRef mediator;

  public static Props createProps() {
    return Props.create(Worker.class);
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting Worker");
    mediator = DistributedPubSub.get(getContext().system()).mediator();
  }

  @Override
  public PartialFunction<Object, BoxedUnit> receive() {
    return ReceiveBuilder
            .match(TaskStorage.TaskIdAssignedMessage.class, this::log, this::calculate)
            .matchAny(this::unhandled)
            .build();
  }

  private boolean log(Object message) {
    log.info("Received message {}", message);
    return true;
  }

  private void calculate(TaskStorage.TaskIdAssignedMessage message) {
    CalculationStarted started = new CalculationStarted(message.getTaskId());
    mediator.tell(new DistributedPubSubMediator.Publish(TaskStorage.TOPIC, started), self());
    CalculationFinished finished = checkIsPrime(message);
    mediator.tell(new DistributedPubSubMediator.Publish(TaskStorage.TOPIC, finished), self());
  }

  /**
   * Calculation doesn't utilize the power of {@link BigInteger#isProbablePrime(int)} on purpose as we need the processing to take longer than 20ms
   */
  public CalculationFinished checkIsPrime(TaskStorage.TaskIdAssignedMessage message) {
    BigInteger n = new BigInteger(message.getNumber());
    log.debug("Checking {}", n);
    if (ZERO.equals(n) || ONE.equals(n) || TWO.equals(n)) {
      return new CalculationFinished(message.getTaskId(), message.getNumber(), true, null);
    }

    BigInteger root = approximateRoot(n);
    log.debug("{}: Using approximate root {}", n, root);

    for (BigInteger divider = TWO; divider.compareTo(root) <= 0; divider = divider.nextProbablePrime()) {
      if (n.mod(divider).equals(ZERO)) {
        log.debug("{}: divides by {}", n, divider);
        return new CalculationFinished(message.getTaskId(), message.getNumber(), false, divider.toString());
      }
    }
    return new CalculationFinished(message.getTaskId(), message.getNumber(), true, null);
  }

  private BigInteger approximateRoot(BigInteger n) {
    BigInteger half = n.shiftRight(1);
    while (half.multiply(half).compareTo(n) > 0) {
      half = half.shiftRight(1);
    }
    return half.shiftLeft(1);
  }

  public static class CalculationStarted implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long taskId;

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

  public static class CalculationFinished implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long taskId;
    private final String number;
    private final boolean isPrime;
    private final String divider;


    public CalculationFinished(long taskId, String number, boolean isPrime, String divider) {
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

    public String getDivider() {
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
