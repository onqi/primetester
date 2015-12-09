package io.onqi.primetester.actors;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import io.onqi.primetester.actors.Worker.CalculationFinished;
import org.junit.AfterClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ResultStorageTest extends JavaTestKit {
  private static final FiniteDuration STORAGE_TIMEOUT = Duration.create(20, TimeUnit.MILLISECONDS);

  private static final ActorSystem system = ActorSystem.create();

  public ResultStorageTest() {
    super(system);
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
  }

  @Test
  public void resultIsStoredOnCalculationFinished() {
    long taskId = 1L;
    String number = "1";
    TestActorRef<ResultStorage> storage = TestActorRef.create(getSystem(), ResultStorage.createProps());

    CalculationFinished msg = new CalculationFinished(taskId, number, true, null);
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getResults()).contains(entry(number, msg));
  }

  @Test
  public void returnsCalculationResult() {
    String number = "1";
    long taskId = 1L;

    TestActorRef<ResultStorage> storage = TestActorRef.create(getSystem(), ResultStorage.createProps());
    storage.underlyingActor().getResults().put(number, new CalculationFinished(taskId, number, true, null));

    ResultStorage.GetCalculationResultMessage msg = new ResultStorage.GetCalculationResultMessage(number);
    storage.tell(msg, getTestActor());


    expectMsgEquals(STORAGE_TIMEOUT, new ResultStorage.CalculationResultMessage(number, true, null));
  }

  @Test
  public void returnsNotFoundIfNoResult() {
    String number = "1";
    TestActorRef<ResultStorage> storage = TestActorRef.create(getSystem(), ResultStorage.createProps());

    ResultStorage.GetCalculationResultMessage msg = new ResultStorage.GetCalculationResultMessage(number);
    storage.tell(msg, getTestActor());


    expectMsgEquals(STORAGE_TIMEOUT, ResultStorage.CalculationResultMessage.NOT_FOUND);

  }
}
