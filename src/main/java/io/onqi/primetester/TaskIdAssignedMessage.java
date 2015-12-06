package io.onqi.primetester;

import java.util.Objects;

public class TaskIdAssignedMessage {
  private static final long serialVersionUID = 1L;

  private final long taskId;
  private final String number;

  public TaskIdAssignedMessage(long taskId, String number) {
    this.taskId = taskId;
    this.number = number;
  }

  public long getTaskId() {
    return taskId;
  }

  public String getNumber() {
    return number;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaskIdAssignedMessage that = (TaskIdAssignedMessage) o;
    return taskId == that.taskId &&
            Objects.equals(number, that.number);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, number);
  }

  @Override
  public String toString() {
    return "TaskIdAssignedMessage{" +
            "taskId=" + taskId +
            ", number='" + number + '\'' +
            '}';
  }
}
