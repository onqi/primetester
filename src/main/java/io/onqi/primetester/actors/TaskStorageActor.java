package io.onqi.primetester.actors;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.NewNumberCalculationMessage;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static akka.actor.ActorRef.noSender;

public class TaskStorageActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  private ActorSelection worker;
  private ActorSelection notificationRegistry;

  private AtomicLong id = new AtomicLong();
  private HashMap<Long, Status> statuses = new HashMap<>();
  private HashMap<Long, String> tasks = new HashMap<>();

  public static Props createProps() {
    return Props.create(TaskStorageActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    if (message instanceof NewNumberCalculationMessage) {
      NewNumberCalculationMessage msg = (NewNumberCalculationMessage) message;
      long taskId = id.incrementAndGet();
      tasks.put(taskId, msg.getNumber());
      statuses.put(taskId, Status.QUEUED);
      TaskIdAssignedMessage taskIdAssignedMessage = new TaskIdAssignedMessage(taskId, msg.getNumber());
      worker.tell(taskIdAssignedMessage, noSender());
      getSender().tell(taskIdAssignedMessage, noSender());

    } else if (message instanceof WorkerActor.CalculationStarted) {
      statuses.put(((WorkerActor.CalculationStarted) message).getTaskId(), Status.STARTED);
      notificationRegistry.tell(message, noSender());

    } else if (message instanceof WorkerActor.CalculationFinished) {
      WorkerActor.CalculationFinished calculationFinished = (WorkerActor.CalculationFinished) message;
      statuses.put(calculationFinished.getTaskId(), Status.FINISHED);
      notificationRegistry.tell(message, noSender());

    } else if (message instanceof GetTaskStatusMessage) {
      long taskId = ((GetTaskStatusMessage) message).getTaskId();

      TaskStatusMessage taskStatus = Optional.ofNullable(statuses.get(taskId))
              .map(st -> new TaskStatusMessage(taskId, tasks.get(taskId), st)).orElse(TaskStatusMessage.NOT_FOUND);
      getSender().tell(taskStatus, self());

    } else {
      unhandled(message);
    }
  }

  @Override
  public void preStart() throws Exception {
    log.debug("Starting TaskStorage");
    worker = context().system().actorSelection(ActorSystemHolder.WORKER_PATH);
    notificationRegistry = context().system().actorSelection(ActorSystemHolder.NOTIFICATION_REGISTRY_PATH);
  }

  @Override
  public void postStop() throws Exception {
    log.debug("TaskStorage stopped");
  }

  HashMap<Long, Status> getStatuses() {
    return statuses;
  }

  HashMap<Long, String> getTasks() {
    return tasks;
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

  public static class GetTaskStatusMessage {
    private static final long serialVersionUID = 1L;

    private final long taskId;

    public GetTaskStatusMessage(long taskId) {
      this.taskId = taskId;
    }

    public long getTaskId() {
      return taskId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      GetTaskStatusMessage that = (GetTaskStatusMessage) o;
      return taskId == that.taskId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId);
    }

    @Override
    public String toString() {
      return "GetTaskStatusMessage{" +
              "taskId=" + taskId +
              '}';
    }
  }

  public static class TaskStatusMessage {
    private static final long serialVersionUID = 1L;
    public static final TaskStatusMessage NOT_FOUND = new TaskStatusMessage(Long.MIN_VALUE, "", null);

    private final long taskId;
    private final String number;
    private final Status status;

    public TaskStatusMessage(long taskId, String number, Status status) {
      this.taskId = taskId;
      this.number = number;
      this.status = status;
    }

    public long getTaskId() {
      return taskId;
    }

    public String getNumber() {
      return number;
    }

    public Status getStatus() {
      return status;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TaskStatusMessage that = (TaskStatusMessage) o;
      return taskId == that.taskId &&
              Objects.equals(number, that.number) &&
              status == that.status;
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId, number, status);
    }

    @Override
    public String toString() {
      return "TaskStatusMessage{" +
              "taskId=" + taskId +
              ", number='" + number + '\'' +
              ", status=" + status +
              '}';
    }
  }
}
