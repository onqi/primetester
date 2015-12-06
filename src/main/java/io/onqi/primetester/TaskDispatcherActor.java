package io.onqi.primetester;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Objects;

public class TaskDispatcherActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  public static Props createProps() {
    return Props.create(StorageActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    unhandled(message);
  }

  @Override
  public void preStart() throws Exception {
    log.debug("Starting TaskDispatcher");
  }

  @Override
  public void postStop() throws Exception {
    log.debug("TaskDispatcher stopped");
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CalculationRequest that = (CalculationRequest) o;
      return id == that.id &&
              Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, number);
    }

    @Override
    public String toString() {
      return "CalculationRequest{" +
              "id=" + id +
              ", number='" + number + '\'' +
              '}';
    }
  }
}
