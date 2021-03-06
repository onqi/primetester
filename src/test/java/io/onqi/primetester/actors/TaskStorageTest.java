package io.onqi.primetester.actors;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import io.onqi.primetester.NewNumberCalculationMessage;
import org.junit.AfterClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static io.onqi.primetester.actors.TaskStorage.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class TaskStorageTest extends JavaTestKit {
  private static final FiniteDuration TASK_STORAGE_TIMEOUT = Duration.create(20, TimeUnit.MILLISECONDS);

  private static final ActorSystem system = ActorSystem.create();

  public TaskStorageTest() {
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
    TestActorRef<TaskStorage> storage = TestActorRef.create(getSystem(), TaskStorage.createProps());

    NewNumberCalculationMessage msg = new NewNumberCalculationMessage(number);
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getTasks()).contains(entry(taskId, number));
    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, TaskStorage.Status.QUEUED));

    expectMsgEquals(TASK_STORAGE_TIMEOUT, new TaskStorage.TaskIdAssignedMessage(taskId, number));
  }

  @Test
  public void statusIsUpdatedToStartedOnCalculationStarted() {
    long taskId = 1L;
    TestActorRef<TaskStorage> storage = TestActorRef.create(getSystem(), TaskStorage.createProps());
    storage.underlyingActor().getStatuses().put(taskId, QUEUED);

    Worker.CalculationStarted msg = new Worker.CalculationStarted(taskId);
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, STARTED));
  }

  @Test
  public void statusIsUpdatedToFinishedOnCalculationFinished() {
    long taskId = 1L;
    TestActorRef<TaskStorage> storage = TestActorRef.create(getSystem(), TaskStorage.createProps());
    storage.underlyingActor().getStatuses().put(taskId, STARTED);

    Worker.CalculationFinished msg = new Worker.CalculationFinished(taskId, "1", true, null);
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, FINISHED));
  }

  @Test
  public void returnsTaskStatus() {
    long taskId = 1L;
    String number = "1";
    TaskStorage.Status status = STARTED;
    TestActorRef<TaskStorage> storage = TestActorRef.create(getSystem(), TaskStorage.createProps());
    storage.underlyingActor().getTasks().put(taskId, number);
    storage.underlyingActor().getStatuses().put(taskId, status);

    TaskStorage.GetTaskStatusMessage msg = new TaskStorage.GetTaskStatusMessage(taskId);
    storage.tell(msg, getTestActor());

    expectMsgEquals(TASK_STORAGE_TIMEOUT, new TaskStorage.TaskStatusMessage(taskId, number, status));
  }

  @Test
  public void returnsNotFoundIfNoTask() {
    long taskId = 1L;
    TestActorRef<TaskStorage> storage = TestActorRef.create(getSystem(), TaskStorage.createProps());

    TaskStorage.GetTaskStatusMessage msg = new TaskStorage.GetTaskStatusMessage(taskId);
    storage.tell(msg, getTestActor());

    expectMsgEquals(TASK_STORAGE_TIMEOUT, TaskStorage.TaskStatusMessage.NOT_FOUND);
  }
}
