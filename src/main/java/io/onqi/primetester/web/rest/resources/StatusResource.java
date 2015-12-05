package io.onqi.primetester.web.rest.resources;

@SuppressWarnings("FieldCanBeLocal")
public class StatusResource {
  private final long taskId;
  private final boolean isFinished;

  public StatusResource(long taskId, boolean isFinished) {
    this.taskId = taskId;
    this.isFinished = isFinished;
  }
}
