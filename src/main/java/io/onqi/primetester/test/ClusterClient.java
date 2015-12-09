package io.onqi.primetester.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.onqi.primetester.actors.ClusterProxy;
import io.onqi.primetester.actors.TaskStorageActor;
import scala.Console;

import java.util.concurrent.TimeUnit;

public class ClusterClient {
  private static LoggingAdapter log;

  public static void main(String[] args) {
    final Config config = ConfigFactory.parseString("akka.cluster.roles = [frontend]")
            .withFallback(ConfigFactory.load());

    ActorSystem system = ActorSystem.create("PrimeTesterCluster", config);
    log = Logging.getLogger(system, ClusterClient.class);

    Cluster cluster = Cluster.get(system);
    ActorRef clusterParent = system.actorOf(ClusterProxy.createProps(), "clusterProxy");
    doMessageLoop(system, cluster, clusterParent);
  }

  @SuppressWarnings("unchecked")
  private static void doMessageLoop(ActorSystem system, Cluster cluster, ActorRef clusterParent) {
    while (!Console.readLine().toString().isEmpty()) {
      Patterns.ask(clusterParent, new TaskStorageActor.TaskIdAssignedMessage(123L, "1234"),
              Timeout.apply(300, TimeUnit.MILLISECONDS))
              .onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable failure, Object success) throws Throwable {
                  if (failure != null) {
                    log.info("Got response {}", success);
                  } else {
                    log.info("Got failure {}", success);
                  }
                }
              }, system.dispatcher());
    }
    cluster.shutdown();
  }
}