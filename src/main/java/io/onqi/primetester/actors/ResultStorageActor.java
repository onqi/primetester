package io.onqi.primetester.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class ResultStorageActor extends UntypedActor {
  private final LoggingAdapter log = Logging.getLogger(context().system(), this);
  private final HashMap<String, WorkerActor.CalculationFinished> results = new HashMap<>();

  public static Props createProps() {
    return Props.create(ResultStorageActor.class);
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting ResultStorageActor");
    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
    mediator.tell(new DistributedPubSubMediator.Subscribe(TaskStorageActor.TOPIC, getSelf()), getSelf());
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.info("Received message {}", message);
    if (message instanceof WorkerActor.CalculationFinished) {
      storeResult((WorkerActor.CalculationFinished) message);

    } else if (message instanceof GetCalculationResultMessage) {
      handleGet((GetCalculationResultMessage) message);

    } else if (message instanceof DistributedPubSubMediator.SubscribeAck) {
      logSubscribeAck();

    } else {
      unhandled(message);
    }
  }

  private void storeResult(WorkerActor.CalculationFinished message) {
    results.put(message.getNumber(), message);
  }

  private void handleGet(GetCalculationResultMessage message) {
    String number = message.number;

    CalculationResultMessage result = Optional.ofNullable(results.get(number))
            .map(cf -> new CalculationResultMessage(cf.getNumber(), cf.isPrime(), cf.getDivider()))
            .orElse(CalculationResultMessage.NOT_FOUND);
    getSender().tell(result, self());
  }

  private void logSubscribeAck() {
    log.info("Successfully subscribed for topic '{}'", TaskStorageActor.TOPIC);
  }

  HashMap<String, WorkerActor.CalculationFinished> getResults() {
    return results;
  }

  public static class GetCalculationResultMessage implements Serializable {
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

  public static class CalculationResultMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final CalculationResultMessage NOT_FOUND = new CalculationResultMessage("", false, null);

    private final String number;
    private final boolean isPrime;
    private final String divider;

    public CalculationResultMessage(String number, boolean isPrime, String divider) {
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

    public String getDivider() {
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



