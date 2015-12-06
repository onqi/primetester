package io.onqi.primetester.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerActorTest extends JavaTestKit {

  public static final long TASK_ID = 1L;

  public static final FiniteDuration TIMEOUT = FiniteDuration.apply(1, TimeUnit.SECONDS);

  public WorkerActorTest() {
    super(ActorSystem.create());
  }

  @Test
  public void primeIsReportedFor1() throws Exception {
    String number = "1";
    StorageActor.TaskIdAssignedMessage msg = new StorageActor.TaskIdAssignedMessage(TASK_ID, number);

    ActorRef worker = getSystem().actorOf(WorkerActor.createProps());
    worker.tell(msg, getTestActor());

    expectMsgEquals(TIMEOUT,
            new WorkerActor.CalculationFinished(TASK_ID, number, true, Optional.empty()));
  }

  @Test
  public void nonPrimeIsReportedFor459() throws Exception {
    String number = "459";
    StorageActor.TaskIdAssignedMessage msg = new StorageActor.TaskIdAssignedMessage(TASK_ID, number);

    ActorRef worker = getSystem().actorOf(WorkerActor.createProps());
    worker.tell(msg, getTestActor());

    expectMsgEquals(TIMEOUT,
            new WorkerActor.CalculationFinished(TASK_ID, number, false, Optional.of("3")));
  }

  @Test
  public void primeIsReportedForRealPrime() throws Exception {
    String number = readPrimes(1).stream().findFirst().orElseThrow(() -> new RuntimeException("failed to read primes"));
    StorageActor.TaskIdAssignedMessage msg = new StorageActor.TaskIdAssignedMessage(TASK_ID, number);

    ActorRef worker = getSystem().actorOf(WorkerActor.createProps());
    worker.tell(msg, getTestActor());

    expectMsgEquals(TIMEOUT,
            new WorkerActor.CalculationFinished(TASK_ID, number, true, Optional.empty()));
  }

  @Test
  public void testPrimesFileExists() {
    assertThat(getClass().getResource("/primes50.txt")).isNotNull();
  }

  private static List<String> readPrimes(int count) throws Exception {
    Stream<String> primesStream = Files.lines(Paths.get(WorkerActorTest.class.getResource("/primes50.txt").toURI()));
    return primesStream.limit(count).collect(Collectors.toList());
  }
}
