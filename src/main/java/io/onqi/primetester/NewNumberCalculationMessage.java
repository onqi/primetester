package io.onqi.primetester;

import java.util.Objects;

public class NewNumberCalculationMessage {
  private static final long serialVersionUID = 1L;

  private final String number;

  public NewNumberCalculationMessage(String number) {
    this.number = number;
  }

  public String getNumber() {
    return number;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NewNumberCalculationMessage that = (NewNumberCalculationMessage) o;
    return Objects.equals(number, that.number);
  }

  @Override
  public int hashCode() {
    return Objects.hash(number);
  }

  @Override
  public String toString() {
    return "NewNumberCalculationMessage{" +
            "number='" + number + '\'' +
            '}';
  }
}
