package io.onqi.primetester.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigInteger;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonSerialize
@JsonInclude(NON_NULL)
public class Result {
  private final String number;
  private final boolean isPrime;
  private final Optional<BigInteger> divider;

  public Result(String number, boolean isPrime, Optional<BigInteger> divider) {
    this.number = number;
    this.isPrime = isPrime;
    this.divider = divider;
  }

  public String getNumber() {
    return number;
  }

  public boolean isPrime() {
    return isPrime;
  }

  public Optional<BigInteger> getDivider() {
    return divider;
  }
}
