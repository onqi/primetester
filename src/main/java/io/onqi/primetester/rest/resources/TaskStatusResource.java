package io.onqi.primetester.rest.resources;

import io.onqi.primetester.actors.TaskStorage;

import java.util.Arrays;

@SuppressWarnings("FieldCanBeLocal")
public class TaskStatusResource {
  private long taskId;
  private String number;
  private Status status;

  private TaskStatusResource() { /* for serialization only */ }

  public TaskStatusResource(long taskId, String number, TaskStorage.Status status) {
    this.taskId = taskId;
    this.number = number;
    this.status = Status.byActorEnum(status);
  }

  public long getTaskId() {
    return taskId;
  }

  public String getNumber() {
    return number;
  }

  public Status getStatus() {
    return status;
  }

  public enum Status {
    STARTED(TaskStorage.Status.STARTED),
    QUEUED(TaskStorage.Status.QUEUED),
    FINISHED(TaskStorage.Status.FINISHED);

    private final TaskStorage.Status status;

    Status(TaskStorage.Status status) {
      this.status = status;
    }

    public static Status byActorEnum(TaskStorage.Status status) {
      return Arrays.stream(Status.values())
              .filter(s -> s.status.equals(status)).findFirst()
              .orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported status '%s'", status)));
    }
  }
}
