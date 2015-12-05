package io.onqi.primetester.web.rest.resources;

import io.onqi.primetester.actors.TaskManagerActor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class CreateTaskResource {
  public static final String NUMBER_PATTERN = "^([0-9]+)$";

  @NotNull
  @Pattern(regexp = NUMBER_PATTERN)
  private String number;

  public TaskManagerActor.CreateTaskRequest toMessage() {
    return new TaskManagerActor.CreateTaskRequest(number);
  }
}
