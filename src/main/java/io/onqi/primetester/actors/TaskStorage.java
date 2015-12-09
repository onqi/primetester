package io.onqi.primetester.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import io.onqi.primetester.ActorSystemHolder;
import io.onqi.primetester.NewNumberCalculationMessage;
import io.onqi.primetester.rest.resources.TaskStatusResource;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static akka.actor.ActorRef.noSender;
import static io.onqi.primetester.actors.TaskStorage.Status.QUEUED;

public class TaskStorage extends AbstractActor {
  public static final String TOPIC = "statusMessages";
  private LoggingAdapter log = Logging.getLogger(context().system(), this);

  private ActorRef clusterProxy;

  private final AtomicLong id = new AtomicLong();
  private final HashMap<Long, Status> statuses = new HashMap<>();
  private final HashMap<Long, String> tasks = new HashMap<>();
  private final Map<Long, SseBroadcaster> subscriptions = new HashMap<>();

  public static Props createProps() {
    return Props.create(TaskStorage.class);
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting TaskStorage");
    clusterProxy = context().system().actorFor(ActorSystemHolder.CLUSTER_PROXY_PATH);
    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
    mediator.tell(new DistributedPubSubMediator.Subscribe(TOPIC, self()), self());
  }

  @Override
  public PartialFunction<Object, BoxedUnit> receive() {
    return ReceiveBuilder
            .match(NewNumberCalculationMessage.class, this::log, this::queueNewMessage)
            .match(RegisterForNotifications.class, this::log, this::registerNewSSESubscription)
            .match(Worker.CalculationStarted.class, this::log, m -> recordStateChange(m.getTaskId(), Status.STARTED))
            .match(Worker.CalculationFinished.class, this::log, m -> recordStateChange(m.getTaskId(), Status.FINISHED))
            .match(GetTaskStatusMessage.class, this::log, this::handleGet)
            .match(DistributedPubSubMediator.SubscribeAck.class, m -> logSubscribeAck())
            .matchAny(this::unhandled)
            .build();
  }

  private boolean log(Object message) {
    log.info("Received message {}", message);
    return true;
  }

  private void handleGet(GetTaskStatusMessage message) {
    long taskId = message.getTaskId();

    TaskStatusMessage taskStatus = Optional.ofNullable(statuses.get(taskId))
            .map(st -> new TaskStatusMessage(taskId, tasks.get(taskId), st)).orElse(TaskStatusMessage.NOT_FOUND);
    sender().tell(taskStatus, self());
  }

  private void recordStateChange(long taskId, Status status) {
    statuses.put(taskId, status);
    broadcast(taskId, status);
  }

  private void queueNewMessage(NewNumberCalculationMessage message) {
    long taskId = id.incrementAndGet();
    tasks.put(taskId, message.getNumber());
    statuses.put(taskId, Status.QUEUED);
    TaskIdAssignedMessage taskIdAssignedMessage = new TaskIdAssignedMessage(taskId, message.getNumber());
    clusterProxy.tell(taskIdAssignedMessage, self());
    sender().tell(taskIdAssignedMessage, noSender());
  }

  private void registerNewSSESubscription(RegisterForNotifications message) {
    subscriptions.put(message.taskId, message.broadcaster);
    Status existingStatus = statuses.get(message.taskId);
    if (!QUEUED.equals(existingStatus)) {
      broadcast(message.taskId, existingStatus);
    }
  }


  private void broadcast(long taskId, TaskStorage.Status status) {
    OutboundEvent event = new OutboundEvent.Builder()
            .id(String.valueOf(taskId))
            .name(status.toString())
            .data(TaskStatusResource.class, new TaskStatusResource(taskId, null, status))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
    Optional.ofNullable(subscriptions.get(taskId)).ifPresent(b -> b.broadcast(event));
  }

  private void logSubscribeAck() {
    log.info("Successfully subscribed for topic '{}'", TOPIC);
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

  public static class RegisterForNotifications implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long taskId;
    private final SseBroadcaster broadcaster;

    public RegisterForNotifications(long taskId, SseBroadcaster broadcaster) {
      this.taskId = taskId;
      this.broadcaster = broadcaster;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RegisterForNotifications that = (RegisterForNotifications) o;
      return taskId == that.taskId &&
              Objects.equals(broadcaster, that.broadcaster);
    }

    @Override
    public int hashCode() {
      return Objects.hash(taskId, broadcaster);
    }

    @Override
    public String toString() {
      return "RegisterForNotifications{" +
              "taskId=" + taskId +
              ", broadcaster=" + broadcaster +
              '}';
    }
  }
}
