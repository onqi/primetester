package io.onqi.primetester;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.AfterClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.onqi.primetester.StorageActor.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class StorageActorTest extends JavaTestKit {
  private static final FiniteDuration STORAGE_TIMEOUT = Duration.create(20, TimeUnit.MILLISECONDS);

  private static final ActorSystem system = ActorSystem.create();

  public StorageActorTest() {
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
    NewNumberCalculationMessage msg = new NewNumberCalculationMessage(number);
    TestActorRef<StorageActor> storage = TestActorRef.create(getSystem(), StorageActor.createProps());

    assertThat(storage.underlyingActor().getStatuses()).isEmpty();
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getResults()).isEmpty();
    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, QUEUED));

    expectMsgEquals(STORAGE_TIMEOUT, new StorageActor.TaskIdAssignedMessage(taskId, number));
  }

  @Test
  public void statusIsUpdatedToStartedOnCalculationStarted() {
    long taskId = 1L;
    WorkerActor.CalculationStarted msg = new WorkerActor.CalculationStarted(taskId);
    TestActorRef<StorageActor> storage = TestActorRef.create(getSystem(), StorageActor.createProps());

    assertThat(storage.underlyingActor().getStatuses().put(taskId, QUEUED)).isNull();
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getResults()).isEmpty();
    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, STARTED));
  }

  @Test
  public void statusIsUpdatedToFinishedAndResultIsStoredOnCalculationFinished() {
    long taskId = 1L;
    String number = "1";
    WorkerActor.CalculationFinished msg = new WorkerActor.CalculationFinished(taskId, number, true, Optional.empty());
    TestActorRef<StorageActor> storage = TestActorRef.create(getSystem(), StorageActor.createProps());

    assertThat(storage.underlyingActor().getStatuses().put(taskId, STARTED)).isNull();
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getResults()).contains(entry(number, msg));
    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, FINISHED));
  }

}
