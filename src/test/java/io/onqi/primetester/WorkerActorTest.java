package io.onqi.primetester;

import io.onqi.primetester.actors.WorkerActor;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerActorTest {

  @Test
  @Ignore("Figure out how to test using actorSystem")
  public void primeIsReported() throws Exception {
    WorkerActor workerActor = new WorkerActor();
    readPrimes(50).parallelStream().forEach(prime -> assertThat(workerActor.checkIsPrime(new BigInteger(prime))).isTrue());
  }

  @Test
  @Ignore("Figure out how to test using actorSystem")
  public void nonPrimeIsReported() throws Exception {
    WorkerActor workerActor = new WorkerActor();
    assertThat(workerActor.checkIsPrime(new BigInteger("9823458762743567243567"))).isFalse();
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
