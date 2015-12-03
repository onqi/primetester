package io.onqi.primetester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class Worker {
  private static final BigInteger THREE = BigInteger.valueOf(3L);
  private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

  /**
   * Calculation doesn't utilize the power of {@link BigInteger#isProbablePrime(int)} on purpose as we need the processing to take longer than 20ms
   */
  public boolean checkIsPrime(BigInteger n) {
    if (n.equals(ONE) || n.equals(ZERO)) {
      return true;
    }

    BigInteger root = approximateRoot(n);
    LOGGER.debug("{}: Using approximate root {}", n, root);

    int cnt = 0;
    for (BigInteger divider = THREE; divider.compareTo(root) <= 0; divider = divider.nextProbablePrime()) {
      cnt++;
      if (cnt % 1000 == 0) {
        LOGGER.trace("{}: {} attempts made. Trying next divider {}", n, cnt, divider);
      }
      if (n.mod(divider).equals(ZERO)) {
        LOGGER.debug("{}: divides by {}", n, divider);
        return false;
      }
    }
    return true;
  }

  private BigInteger approximateRoot(BigInteger n) {
    BigInteger half = n.shiftRight(1);
    while (half.multiply(half).compareTo(n) > 0) {
      half = half.shiftRight(1);
    }
    return half.shiftLeft(1);
  }
}
