package io.onqi.primetester.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.NewNumberCalculationMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static akka.actor.ActorRef.noSender;

public class TaskStorageActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  private ActorRef clusterProxy;

  private AtomicLong id = new AtomicLong();
  private HashMap<Long, Status> statuses = new HashMap<>();
  private HashMap<Long, String> tasks = new HashMap<>();

  public static Props createProps() {
    return Props.create(TaskStorageActor.class);
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting TaskStorage");
    clusterProxy = context().system().actorFor(ActorSystemHolder.CLUSTER_PROXY_PATH);
    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
    mediator.tell(new DistributedPubSubMediator.Subscribe(NotificationRegistryActor.TOPIC, getSelf()), getSelf());
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.info("Received message {}", message);
    if (message instanceof NewNumberCalculationMessage) {
      queueNewMessage((NewNumberCalculationMessage) message);

    } else if (message instanceof WorkerActor.CalculationStarted) {
      recordStateChange(((WorkerActor.CalculationStarted) message).getTaskId(), Status.STARTED);

    } else if (message instanceof WorkerActor.CalculationFinished) {
      recordStateChange(((WorkerActor.CalculationFinished) message).getTaskId(), Status.FINISHED);

    } else if (message instanceof GetTaskStatusMessage) {
      handleGet((GetTaskStatusMessage) message);

    } else if (message instanceof DistributedPubSubMediator.SubscribeAck) {
      logSubscribeAck();

    } else {
      unhandled(message);
    }
  }


  @Override
  public void postStop() throws Exception {
    log.info("TaskStorage stopped");
  }

  private void handleGet(GetTaskStatusMessage message) {
    long taskId = message.getTaskId();

    TaskStatusMessage taskStatus = Optional.ofNullable(statuses.get(taskId))
            .map(st -> new TaskStatusMessage(taskId, tasks.get(taskId), st)).orElse(TaskStatusMessage.NOT_FOUND);
    getSender().tell(taskStatus, self());
  }

  private void recordStateChange(long taskId, Status started) {
    statuses.put(taskId, started);
  }

  private void queueNewMessage(NewNumberCalculationMessage message) {
    long taskId = id.incrementAndGet();
    tasks.put(taskId, message.getNumber());
    statuses.put(taskId, Status.QUEUED);
    TaskIdAssignedMessage taskIdAssignedMessage = new TaskIdAssignedMessage(taskId, message.getNumber());
    clusterProxy.tell(taskIdAssignedMessage, getSelf());
    getSender().tell(taskIdAssignedMessage, noSender());
  }

  private void logSubscribeAck() {
    log.info("Successfully subscribed for topic '{}'", NotificationRegistryActor.TOPIC);
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

  public static class TaskIdAssignedMessage implements Serializable {
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

  public static class GetTaskStatusMessage implements Serializable {
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

  public static class TaskStatusMessage implements Serializable {
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
