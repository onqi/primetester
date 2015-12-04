package io.onqi.primetester.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class RegistrarActor extends UntypedActor {

  public static Props createProps() {
    return Props.create(RegistrarActor.class);
  }

  @Override
  public void onReceive(Object message) throws Exception {
    getSender().tell(123L, getSelf());
  }
}
