package io.onqi.primetester;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.WorkerActor.CalculationFinished;

import java.util.HashMap;
import java.util.Objects;
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
  private HashMap<String, WorkerActor.CalculationFinished> results = new HashMap<>();

  public static Props createProps() {
    return Props.create(StorageActor.class);
  }

  /**
   * {@link CalculationFinished} from {@link io.onqi.primetester.WorkerActor} to persist calculation result<br/>
   */
  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof NewNumberCalculationMessage) {
      NewNumberCalculationMessage msg = (NewNumberCalculationMessage) message;
      long taskId = id.incrementAndGet();
      statuses.put(taskId, Status.QUEUED);
      getSender().tell(new TaskIdAssignedMessage(taskId, msg.getNumber()), noSender());

    } else if (message instanceof WorkerActor.CalculationStarted) {
      statuses.put(((WorkerActor.CalculationStarted) message).getTaskId(), Status.STARTED);

    } else if (message instanceof CalculationFinished) {
      WorkerActor.CalculationFinished calculationFinished = (CalculationFinished) message;

      results.put(calculationFinished.getNumber(), calculationFinished);
      statuses.put(calculationFinished.getTaskId(), Status.FINISHED);

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

  HashMap<String, WorkerActor.CalculationFinished> getResults() {
    return results;
  }

  public enum Status {
    QUEUED,
    STARTED,
    FINISHED
  }

  public static class TaskIdAssignedMessage {
    private static final long serialVersionUID = 1L;

    private final long taskId;
    private final String number;

    public TaskIdAssignedMessage(long taskId, String number) {
      this.taskId = taskId;
      this.number = number;
    }

    public long getTaskId() {
      return taskId;
    }

    public String getNumber() {
      return number;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TaskIdAssignedMessage that = (TaskIdAssignedMessage) o;
      return taskId == that.taskId &&
              Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId, number);
    }

    @Override
    public String toString() {
      return "TaskIdAssignedMessage{" +
              "taskId=" + taskId +
              ", number='" + number + '\'' +
              '}';
    }
  }
}



