package io.onqi.primetester.rest;

import io.onqi.primetester.actors.TaskStorageActor;

import java.util.Arrays;

@SuppressWarnings("FieldCanBeLocal")
public class TaskStatusResource {
  private long taskId;
  private Status status;

  private TaskStatusResource() { /* for serialization only */ }

  public TaskStatusResource(long taskId, TaskStorageActor.Status status) {
    this.taskId = taskId;
    this.status = Status.byActorEnum(status);
  }

  public long getTaskId() {
    return taskId;
  }

  public Status getStatus() {
    return status;
  }

  public enum Status {
    STARTED(TaskStorageActor.Status.STARTED),
    QUEUED(TaskStorageActor.Status.QUEUED),
    FINISHED(TaskStorageActor.Status.FINISHED);

    private final TaskStorageActor.Status status;

    Status(TaskStorageActor.Status status) {
      this.status = status;
    }

    public static Status byActorEnum(TaskStorageActor.Status status) {
      return Arrays.stream(Status.values())
              .filter(s -> s.status.equals(status)).findFirst()
              .orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported status '%s'", status)));
    }
  }
}
