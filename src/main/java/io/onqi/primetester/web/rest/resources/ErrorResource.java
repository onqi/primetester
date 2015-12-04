package io.onqi.primetester.web.rest.resources;

import java.util.Optional;

public class ErrorResource {
  private final Optional<String> error;

  public ErrorResource(Optional<String> error) {
    this.error = error;
  }
}
