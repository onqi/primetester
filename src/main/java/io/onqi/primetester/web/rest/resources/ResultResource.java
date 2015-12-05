package io.onqi.primetester.web.rest.resources;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Optional;

@JsonPropertyOrder({"number", "prime", "divider"})
public class ResultResource {
  private final String number;
  private final boolean prime;
  private final Optional<String> divider;

  public ResultResource(String number, boolean prime, Optional<String> divider) {
    this.number = number;
    this.prime = prime;
    this.divider = divider;
  }

  public String getNumber() {
    return number;
  }

  public boolean isPrime() {
    return prime;
  }

  public Optional<String> getDivider() {
    return divider;
  }
}
