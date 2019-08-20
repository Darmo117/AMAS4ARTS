package amas_traffic.amak.agents.network;

import java.util.List;
import java.util.Random;

import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.agents.MobileEntity;
import amas_traffic.amak.agents.NamedAgent;
import amas_traffic.amak.agents.ObservationZone;

public abstract class ObservableAgent extends NamedAgent {
  private static final Random RANDOM = new Random();

  private ObservationZone observer;
  private int executionOrder;
  protected boolean messageSent;

  public ObservableAgent(TrafficAmas amas, String name) {
    super(amas, name);
    resetExecutionOrder();
  }

  public ObservationZone getObserver() {
    return this.observer;
  }

  public void setObserver(ObservationZone oz) {
    this.observer = oz;
  }

  public boolean isObserved() {
    return this.observer != null;
  }

  public abstract List<MobileEntity> getMobileEntities();

  @Override
  public int getExecutionOrder() {
    return this.executionOrder;
  }

  public void resetExecutionOrder() {
    this.executionOrder = RANDOM.nextInt(999_998) + 2;
  }

  public boolean hasSentMessage() {
    return this.messageSent;
  }

  public void resetMessageSent() {
    this.messageSent = false;
  }
}
