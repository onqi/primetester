package io.onqi.primetester;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import scala.Console;

public class Main {
  public static void main(String[] args) {
    ActorSystemHolder systemHolder = new ActorSystemHolder();
    ActorSelection taskDispatcher = systemHolder.getSystem().actorSelection(ActorSystemHolder.TASK_DISPATCHER_PATH);
    Console.println("Type a number to test or anything else to quit");
    String number;
    while ((number = Console.readLine()).matches("^[0-9]+$")) {
      taskDispatcher.tell(new NewNumberCalculationMessage(number), ActorRef.noSender());
    }

    systemHolder.shutdown();
  }
}
