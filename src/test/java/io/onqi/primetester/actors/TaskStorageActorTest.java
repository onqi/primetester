package io.onqi.primetester.actors;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import io.onqi.primetester.NewNumberCalculationMessage;
import org.junit.AfterClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.onqi.primetester.actors.TaskStorageActor.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class TaskStorageActorTest extends JavaTestKit {
  private static final FiniteDuration TASK_STORAGE_TIMEOUT = Duration.create(20, TimeUnit.MILLISECONDS);

  private static final ActorSystem system = ActorSystem.create();

  public TaskStorageActorTest() {
    super(system);
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
  }

  @Test
  public void statusIsCreatedAsQueuedOnNewCalculation() {
    long taskId = 1L;
    String number = "1";
    TestActorRef<TaskStorageActor> storage = TestActorRef.create(getSystem(), TaskStorageActor.createProps());

    NewNumberCalculationMessage msg = new NewNumberCalculationMessage(number);
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getTasks()).contains(entry(taskId, number));
    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, TaskStorageActor.Status.QUEUED));

    expectMsgEquals(TASK_STORAGE_TIMEOUT, new TaskStorageActor.TaskIdAssignedMessage(taskId, number));
  }

  @Test
  public void statusIsUpdatedToStartedOnCalculationStarted() {
    long taskId = 1L;
    TestActorRef<TaskStorageActor> storage = TestActorRef.create(getSystem(), TaskStorageActor.createProps());
    storage.underlyingActor().getStatuses().put(taskId, QUEUED);

    WorkerActor.CalculationStarted msg = new WorkerActor.CalculationStarted(taskId);
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, STARTED));
  }

  @Test
  public void statusIsUpdatedToFinishedOnCalculationFinished() {
    long taskId = 1L;
    TestActorRef<TaskStorageActor> storage = TestActorRef.create(getSystem(), TaskStorageActor.createProps());
    storage.underlyingActor().getStatuses().put(taskId, STARTED);

    WorkerActor.CalculationFinished msg = new WorkerActor.CalculationFinished(taskId, "1", true, Optional.empty());
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, FINISHED));
  }

  @Test
  public void returnsTaskStatus() {
    long taskId = 1L;
    String number = "1";
    TaskStorageActor.Status status = STARTED;
    TestActorRef<TaskStorageActor> storage = TestActorRef.create(getSystem(), TaskStorageActor.createProps());
    storage.underlyingActor().getTasks().put(taskId, number);
    storage.underlyingActor().getStatuses().put(taskId, status);

    TaskStorageActor.GetTaskStatusMessage msg = new TaskStorageActor.GetTaskStatusMessage(taskId);
    storage.tell(msg, getTestActor());

    expectMsgEquals(TASK_STORAGE_TIMEOUT, new TaskStorageActor.TaskStatusMessage(taskId, number, status));
  }

  @Test
  public void returnsNotFoundIfNoTask() {
    long taskId = 1L;
    TestActorRef<TaskStorageActor> storage = TestActorRef.create(getSystem(), TaskStorageActor.createProps());

    TaskStorageActor.GetTaskStatusMessage msg = new TaskStorageActor.GetTaskStatusMessage(taskId);
    storage.tell(msg, getTestActor());

    expectMsgEquals(TASK_STORAGE_TIMEOUT, TaskStorageActor.TaskStatusMessage.NOT_FOUND);
  }
}
