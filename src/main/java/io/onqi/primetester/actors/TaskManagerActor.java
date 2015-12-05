package io.onqi.primetester.actors;

import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.onqi.primetester.web.rest.ActorSystemHolder;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static akka.actor.ActorRef.noSender;

public class TaskManagerActor extends UntypedActor {
  private LoggingAdapter log = Logging.getLogger(context().system(), this);
  private ActorSelection workerRouter;
  private ActorSelection storage;
  private AtomicLong id = new AtomicLong();
  private HashMap<Long, Boolean> tasks = new HashMap<>();

  public static Props createProps() {
    return Props.create(TaskManagerActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    log.debug("Received message {}", message);
    /* res from TasksEndpoint.createTask */
    if (message instanceof CreateTaskRequest) {
      CreateTaskRequest createTask = (CreateTaskRequest) message;

      long taskId = this.id.incrementAndGet();
      tasks.put(taskId, false);
      workerRouter.tell(new WorkerActor.CalculationRequest(taskId, createTask.number), getSelf());

      getSender().tell(new TaskIdMessage(taskId), noSender());

    /* res from TasksEndpoint.getStatus */
    } else if (message instanceof TaskIdMessage) {
      long taskId = ((TaskIdMessage) message).taskId;
      StatusResponse statusResponse = Optional.ofNullable(tasks.get(taskId))
              .map(finished -> new StatusResponse(taskId, finished)).orElse(StatusResponse.NOT_FOUND);

      getSender().tell(statusResponse, noSender());

    /* no-res from WorkerActor.onReceive */
    } else if (message instanceof WorkerActor.CalculationResponse) {
      WorkerActor.CalculationResponse response = (WorkerActor.CalculationResponse) message;
      tasks.put(response.getId(), true);
      storage.tell(message, noSender());

    } else {
      unhandled(message);
    }
  }

  @Override
  public void preStart() throws Exception {
    log.info("Starting TaskManagerActor");
    storage = context().system().actorSelection(ActorSystemHolder.STORAGE_PATH);
    workerRouter = context().system().actorSelection(ActorSystemHolder.WORKER_ROUTER_PATH);
  }

  @Override
  public void postStop() throws Exception {
    log.info("TaskManagerActor stopped");
    workerRouter.tell(PoisonPill.getInstance(), noSender());
    storage.tell(PoisonPill.getInstance(), noSender());
  }

  public static class CreateTaskRequest {
    private static final long serialVersionUID = 1L;

    private final String number;

    public CreateTaskRequest(String number) {
      this.number = number;
    }
  }

  public static class TaskIdMessage {
    private static final long serialVersionUID = 1L;

    private final long taskId;

    public TaskIdMessage(long taskId) {
      this.taskId = taskId;
    }

    public long getTaskId() {
      return taskId;
    }
  }

  public static class StatusResponse extends TaskIdMessage {
    private static final long serialVersionUID = 1L;
    public static final StatusResponse NOT_FOUND = new StatusResponse(Long.MIN_VALUE, null);

    private final Boolean isFinished;

    public StatusResponse(long taskId, Boolean isFinished) {
      super(taskId);
      this.isFinished = isFinished;
    }

    public Boolean isFinished() {
      return isFinished;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StatusResponse that = (StatusResponse) o;
      return Objects.equals(isFinished, that.isFinished);
    }

    @Override
    public int hashCode() {
      return Objects.hash(isFinished);
    }
  }
}
