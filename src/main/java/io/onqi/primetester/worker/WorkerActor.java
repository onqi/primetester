package io.onqi.primetester.worker;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.perf4j.slf4j.Slf4JStopWatch;

import java.math.BigInteger;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class WorkerActor extends UntypedActor {
  private static final BigInteger THREE = BigInteger.valueOf(3L);
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public static Props createProps() {
    return Props.create(WorkerActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    if (message instanceof BigInteger) {
      log.debug("received message {}" + message);
      getSender().tell(checkIsPrime((BigInteger) message), getSelf());
    } else {
      unhandled(message);
    }
  }

  /**
   * Calculation doesn't utilize the power of {@link BigInteger#isProbablePrime(int)} on purpose as we need the processing to take longer than 20ms
   */
  public boolean checkIsPrime(BigInteger n) {
    Slf4JStopWatch stopWatch = new Slf4JStopWatch("worker");
    try {
      if (n.equals(ONE) || n.equals(ZERO)) {
        return true;
      }

      BigInteger root = approximateRoot(n);
      log.debug("{}: Using approximate root {}", n, root);

      for (BigInteger divider = THREE; divider.compareTo(root) <= 0; divider = divider.nextProbablePrime()) {
        if (n.mod(divider).equals(ZERO)) {
          log.debug("{}: divides by {}", n, divider);
          return false;
        }
      }
      return true;
    } finally {
      stopWatch.stop();
    }
  }

  private BigInteger approximateRoot(BigInteger n) {
    BigInteger half = n.shiftRight(1);
    while (half.multiply(half).compareTo(n) > 0) {
      half = half.shiftRight(1);
    }
    return half.shiftLeft(1);
  }
}
