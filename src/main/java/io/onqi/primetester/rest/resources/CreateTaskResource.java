package io.onqi.primetester.rest.resources;

import io.onqi.primetester.NewNumberCalculationMessage;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class CreateTaskResource {
  public static final String NUMBER_PATTERN = "^([0-9]+)$";

  @NotNull
  @Pattern(regexp = NUMBER_PATTERN)
  private String number;

  private CreateTaskResource() { /* for serialization only */
  }

  public CreateTaskResource(String number) {
    this.number = number;
  }

  public NewNumberCalculationMessage toMessage() {
    return new NewNumberCalculationMessage(number);
  }
}
