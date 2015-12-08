package io.onqi.primetester;

import akka.actor.ActorSystem;
import io.onqi.primetester.actors.NotificationRegistryActor;
import io.onqi.primetester.actors.ResultStorageActor;
import io.onqi.primetester.actors.TaskStorageActor;
import io.onqi.primetester.actors.WorkerActor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ActorSystemHolder {
  private static final String WORKER_NAME = "worker";
  private static final String RESULT_STORAGE_NAME = "resultStorage";
  private static final String TASK_STORAGE_NAME = "taskStorage";
  private static final String NOTIFICATION_REGISTRY_NAME = "notificationRegistry";
  private static final String USER = "/user/";

  public static final String WORKER_PATH = USER + WORKER_NAME;
  public static final String RESULT_STORAGE_PATH = USER + RESULT_STORAGE_NAME;
  public static final String TASK_STORAGE_PATH = USER + TASK_STORAGE_NAME;
  public static final String NOTIFICATION_REGISTRY_PATH = USER + NOTIFICATION_REGISTRY_NAME;


  private final ActorSystem system;

  public ActorSystemHolder() {
    system = ActorSystem.create("primetester");
    system.actorOf(WorkerActor.createProps(), WORKER_NAME);
    system.actorOf(TaskStorageActor.createProps(), TASK_STORAGE_NAME);
    system.actorOf(ResultStorageActor.createProps(), RESULT_STORAGE_NAME);
    system.actorOf(NotificationRegistryActor.createProps(), NOTIFICATION_REGISTRY_NAME);
  }

  public void shutdown() {
    system.shutdown();
    system.awaitTermination(Duration.create(5, TimeUnit.SECONDS));
  }


  public AbstractBinder binder() {
    return new AbstractBinder() {
      protected void configure() {
        bind(system).to(ActorSystem.class);
      }
    };
  }

  ActorSystem getSystem() {
    return system;
  }
}
