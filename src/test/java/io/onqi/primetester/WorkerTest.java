package io.onqi.primetester;

import org.junit.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerTest {

  @Test
  public void primeIsReported() throws Exception {
    Worker worker = new Worker();
    readPrimes(1).stream().forEach(prime -> assertThat(worker.checkIsPrime(new BigInteger(prime))).isTrue());
  }

  @Test
  public void nonPrimeIsReported() throws Exception {
    Worker worker = new Worker();
    assertThat(worker.checkIsPrime(new BigInteger("9823458762743567243567"))).isFalse();
  }

  @Test
  public void testPrimesFileExists() {
    assertThat(getClass().getResource("/primes50.txt")).isNotNull();
  }

  private static List<String> readPrimes(int count) throws Exception {
    Stream<String> primesStream = Files.lines(Paths.get(WorkerTest.class.getResource("/primes50.txt").toURI()));
    return primesStream.limit(count).collect(Collectors.toList());
  }
}
