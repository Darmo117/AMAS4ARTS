package amas_traffic.amak.agents.messaging;

import amas_traffic.amak.agents.NamedAgent;
import fr.irit.smac.amak.messaging.IAmakMessage;

public abstract class MessageImpl<A extends NamedAgent> implements IAmakMessage {
  private final A sender;

  public MessageImpl(A sender) {
    this.sender = sender;
  }

  public A getSender() {
    return this.sender;
  }
}
