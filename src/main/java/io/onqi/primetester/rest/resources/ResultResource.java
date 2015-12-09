package io.onqi.primetester.rest.resources;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"number", "prime", "divider"})
public class ResultResource {
  private String number;
  private boolean prime;
  private String divider;

  private ResultResource() { /* for serialization only */ }

  public ResultResource(String number, boolean prime, String divider) {
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

  public String getDivider() {
    return divider;
  }
}
