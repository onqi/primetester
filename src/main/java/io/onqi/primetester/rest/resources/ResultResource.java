package io.onqi.primetester.rest.resources;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Optional;

@JsonPropertyOrder({"number", "prime", "divider"})
public class ResultResource {
  private String number;
  private boolean prime;
  private Optional<String> divider;

  private ResultResource() { /* for serialization only */ }

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
