package io.onqi.primetester.test;

import io.onqi.primetester.WorkerCluster;

public class SameJvm {
  public static void main(String[] args) {
    WorkerCluster.AllInOne.main(args);
    ClusterClient.main(args);
  }
}
