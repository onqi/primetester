package io.onqi.primetester.web.rest;

import akka.actor.ActorSystem;
import akka.routing.BalancingPool;
import io.onqi.primetester.actors.NotifierActor;
import io.onqi.primetester.actors.StorageActor;
import io.onqi.primetester.actors.TaskManagerActor;
import io.onqi.primetester.actors.WorkerActor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ActorSystemHolder {
  private static final String USER = "/user/";
  private static final String SYSTEM_NAME = "primetester";
  private static final String TASK_MANAGER_NAME = "taskManager";
  private static final String WORKER_ROUTER_NAME = "workerRouter";
  private static final String STORAGE_NAME = "storage";
  private static final String NOTIFIER_ROUTER_NAME = "notifierRouter";
  public static final String TASK_MANAGER_PATH = USER + TASK_MANAGER_NAME;
  public static final String WORKER_ROUTER_PATH = USER + WORKER_ROUTER_NAME;
  public static final String STORAGE_PATH = USER + STORAGE_NAME;
  public static final String NOTIFIER_ROUTER_PATH = USER + NOTIFIER_ROUTER_NAME;

  private ActorSystem system;

  public ActorSystemHolder() {
    system = ActorSystem.create(SYSTEM_NAME);
    system.actorOf(TaskManagerActor.createProps(), TASK_MANAGER_NAME);
    system.actorOf(WorkerActor.createProps(), WORKER_ROUTER_NAME);
//    system.actorOf(WorkerActor.createProps().withRouter(new BalancingPool(10)), WORKER_ROUTER_NAME);
    system.actorOf(StorageActor.createProps(), STORAGE_NAME);
    system.actorOf(NotifierActor.createProps().withRouter(new BalancingPool(10)), NOTIFIER_ROUTER_NAME);
  }

  public void shutdown() {
    system.shutdown();
    system.awaitTermination(Duration.create(15, TimeUnit.SECONDS));
  }

  public AbstractBinder binder() {
    return new AbstractBinder() {
      protected void configure() {
        bind(system).to(ActorSystem.class);
      }
    };
  }
}
