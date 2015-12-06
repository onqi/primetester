package io.onqi.primetester;

import akka.actor.ActorSystem;
import io.onqi.primetester.actors.StorageActor;
import io.onqi.primetester.actors.TaskDispatcherActor;
import io.onqi.primetester.actors.WorkerActor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ActorSystemHolder {
  private static final String WORKER_NAME = "worker";
  private static final String STORAGE_NAME = "storage";
  private static final String TASK_DISPATCHER_NAME = "taskDispatcher";
  private static final String USER = "/user/";

  public static final String WORKER_PATH = USER + WORKER_NAME;
  public static final String STORAGE_PATH = USER + STORAGE_NAME;
  public static final String TASK_DISPATCHER_PATH = USER + TASK_DISPATCHER_NAME;


  private final ActorSystem system;

  public ActorSystemHolder() {
    system = ActorSystem.create("primetester");
    initActors();
  }

  public void shutdown() {
    system.shutdown();
    system.awaitTermination(Duration.create(15, TimeUnit.SECONDS));
  }

  ActorSystem getSystem() {
    return system;
  }

  private void initActors() {
    system.actorOf(WorkerActor.createProps(), WORKER_NAME);
    system.actorOf(StorageActor.createProps(), STORAGE_NAME);
    system.actorOf(TaskDispatcherActor.createProps(), TASK_DISPATCHER_NAME);
  }
}
