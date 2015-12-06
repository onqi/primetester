package io.onqi.primetester;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.Test;

import java.util.Optional;

import static io.onqi.primetester.StorageActor.Status.FINISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class StorageActorTest extends JavaTestKit {
  public StorageActorTest() {
    super(ActorSystem.create());
  }

  @Test
  public void resultAndStatusAreStored() {
    long taskId = 1L;
    String number = "1";
    WorkerActor.CalculationResult msg = new WorkerActor.CalculationResult(taskId, number, true, Optional.empty());
    TestActorRef<StorageActor> storage = TestActorRef.create(getSystem(), StorageActor.createProps());
    storage.tell(msg, getTestActor());

    assertThat(storage.underlyingActor().getResults()).contains(entry(number, msg));
    assertThat(storage.underlyingActor().getStatuses()).contains(entry(taskId, FINISHED));
  }

}
