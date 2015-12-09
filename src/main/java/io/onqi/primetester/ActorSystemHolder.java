package io.onqi.primetester;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.onqi.primetester.actors.ClusterProxy;
import io.onqi.primetester.actors.ResultStorage;
import io.onqi.primetester.actors.TaskStorage;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ActorSystemHolder {
  private static final String CLUSTER_PROXY_NAME = "clusterProxy";
  private static final String RESULT_STORAGE_NAME = "resultStorage";
  private static final String TASK_STORAGE_NAME = "taskStorage";
  private static final String USER = "/user/";

  public static final String CLUSTER_PROXY_PATH = USER + CLUSTER_PROXY_NAME;
  public static final String RESULT_STORAGE_PATH = USER + RESULT_STORAGE_NAME;
  public static final String TASK_STORAGE_PATH = USER + TASK_STORAGE_NAME;

  private final ActorSystem system;

  public ActorSystemHolder() {
    final Config config = ConfigFactory.parseString("akka.cluster.roles = [frontend]")
            .withFallback(ConfigFactory.load());

    system = ActorSystem.create("PrimeTesterCluster", config);

    system.actorOf(ClusterProxy.createProps(), CLUSTER_PROXY_NAME);
    system.actorOf(TaskStorage.createProps(), TASK_STORAGE_NAME);
    system.actorOf(ResultStorage.createProps(), RESULT_STORAGE_NAME);
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
}
