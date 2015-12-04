package io.onqi.primetester.web.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigInteger;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class PrimeTestResource {
  private Long id;
  private String number;
  private Optional<Boolean> isPrime;
  private Optional<BigInteger> divider;

  private PrimeTestResource() {
  }

  public PrimeTestResource(Long id, String number, Optional<Boolean> isPrime, Optional<BigInteger> divider) {
    this.id = id;
    this.number = number;
    this.isPrime = isPrime;
    this.divider = divider;
  }

  public Long getId() {
    return id;
  }

  public String getNumber() {
    return number;
  }

  public Optional<Boolean> getIsPrime() {
    return isPrime;
  }

  public Optional<BigInteger> getDivider() {
    return divider;
  }
}
