package io.onqi.primetester;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.onqi.primetester.actors.WorkerActor;

public class WorkerCluster {
  public static final String WORKER_NAME = "worker";

  public static class AllInOne {
    public static void main(String[] args) {
      initWorkerNode("2551");
      initWorkerNode("2552");
      initWorkerNode("0");
    }
  }

  public static class SingleNode {
    public static void main(String[] args) {
      final String port = args.length > 0 ? args[0] : "0";
      initWorkerNode(port);
    }
  }

  private static void initWorkerNode(String port) {
    final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
            .withFallback(ConfigFactory.parseString("akka.cluster.roles = [worker]"))
            .withFallback(ConfigFactory.load());

    ActorSystem system = ActorSystem.create("PrimeTesterCluster", config);

    system.actorOf(WorkerActor.createProps(), WORKER_NAME);

  }
}
