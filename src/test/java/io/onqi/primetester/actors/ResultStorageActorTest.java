package io.onqi.primetester.actors;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import io.onqi.primetester.actors.WorkerActor.CalculationFinished;
import org.junit.AfterClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ResultStorageActorTest extends JavaTestKit {
  private static final FiniteDuration STORAGE_TIMEOUT = Duration.create(20, TimeUnit.MILLISECONDS);

  private static final ActorSystem system = ActorSystem.create();

  public ResultStorageActorTest() {
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
    TestActorRef<ResultStorageActor> storage = TestActorRef.create(getSystem(), ResultStorageActor.createProps());

    CalculationFinished msg = new CalculationFinished(taskId, number, true, Optional.empty());
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getResults()).contains(entry(number, msg));
  }

  @Test
  public void returnsCalculationResult() {
    String number = "1";
    long taskId = 1L;
    Optional<String> divider = Optional.empty();
    boolean isPrime = true;

    TestActorRef<ResultStorageActor> storage = TestActorRef.create(getSystem(), ResultStorageActor.createProps());
    storage.underlyingActor().getResults().put(number, new CalculationFinished(taskId, number, isPrime, divider));

    ResultStorageActor.GetCalculationResultMessage msg = new ResultStorageActor.GetCalculationResultMessage(number);
    storage.tell(msg, getTestActor());


    expectMsgEquals(STORAGE_TIMEOUT, new ResultStorageActor.CalculationResultMessage(number, isPrime, divider));
  }

  @Test
  public void returnsNotFoundIfNoResult() {
    String number = "1";
    TestActorRef<ResultStorageActor> storage = TestActorRef.create(getSystem(), ResultStorageActor.createProps());

    ResultStorageActor.GetCalculationResultMessage msg = new ResultStorageActor.GetCalculationResultMessage(number);
    storage.tell(msg, getTestActor());


    expectMsgEquals(STORAGE_TIMEOUT, ResultStorageActor.CalculationResultMessage.NOT_FOUND);

  }
}
