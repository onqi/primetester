package io.onqi.primetester;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.Console;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) {
    ActorSystemHolder systemHolder = new ActorSystemHolder();
    final LoggingAdapter log = Logging.getLogger(systemHolder.getSystem(), Main.class);
    ActorSelection taskStorage = systemHolder.getSystem().actorSelection(ActorSystemHolder.TASK_STORAGE_PATH);
    Console.println("Type a number to test or anything else to quit");
    String number;
    while ((number = Console.readLine()).matches("^[0-9]+$")) {
      Future<Object> future = Patterns.ask(taskStorage, new NewNumberCalculationMessage(number), Timeout.apply(2, TimeUnit.SECONDS));
      future.onComplete(new ResultCallback(log), systemHolder.getSystem().dispatcher());
    }

    systemHolder.shutdown();
  }

  private static class ResultCallback extends OnComplete<Object> {
    private final LoggingAdapter log;

    public ResultCallback(LoggingAdapter log) {
      this.log = log;
    }

    @Override
    public void onComplete(Throwable failure, Object success) throws Throwable {
      if (failure != null) {
        log.error(failure, "failed to assign a task");
      } else {
        log.debug("Assigned task '{}'", success);
      }
    }
  }
}
