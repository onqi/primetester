package io.onqi.primetester.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerActorTest extends JavaTestKit {

  public static final long TASK_ID = 1L;

  public WorkerActorTest() {
    super(ActorSystem.create());
  }

  @Test
  public void primeIsReportedFor1() throws Exception {
    String number = "1";
    TaskStorageActor.TaskIdAssignedMessage msg = new TaskStorageActor.TaskIdAssignedMessage(TASK_ID, number);

    ActorRef worker = getSystem().actorOf(WorkerActor.createProps());
    worker.tell(msg, getTestActor());

    //TODO expect CalculationFinished
  }

  @Test
  public void nonPrimeIsReportedFor459() throws Exception {
    String number = "459";
    TaskStorageActor.TaskIdAssignedMessage msg = new TaskStorageActor.TaskIdAssignedMessage(TASK_ID, number);

    ActorRef worker = getSystem().actorOf(WorkerActor.createProps());
    worker.tell(msg, getTestActor());

    //TODO expect CalculationFinished
  }

  @Test
  public void primeIsReportedForRealPrime() throws Exception {
    String number = readPrimes(1).stream().findFirst().orElseThrow(() -> new RuntimeException("failed to read primes"));
    TaskStorageActor.TaskIdAssignedMessage msg = new TaskStorageActor.TaskIdAssignedMessage(TASK_ID, number);

    ActorRef worker = getSystem().actorOf(WorkerActor.createProps());
    worker.tell(msg, getTestActor());

    //TODO expect CalculationFinished
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
