package amas_traffic.amak.agents.network;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.agents.MobileEntity;
import amas_traffic.amak.agents.messaging.EdgeCriticalitiesMessage;
import amas_traffic.amak.agents.messaging.UpdateEdgeCriticalitiesMessage;
import fr.irit.smac.amak.messaging.IAmakMessage;

/**
 * This type of agent represents a graph edge. Edges can be sorted based on
 * their criticality.
 * 
 * @author Damien Vergnet
 */
public class Edge extends ObservableAgent implements Comparable<Edge> {
  private final Node source;
  private final Node target;
  private final int maxCapacity;
  private int expectedEntitiesNumber;
  private Map<CriticalityType, Double> criticalities;

  public Edge(TrafficAmas amas, String name, Node source, Node target, int maxCapacity) {
    super(amas, name);
    this.source = source;
    this.target = target;
    this.maxCapacity = maxCapacity;
    this.criticalities = new HashMap<>();
    Arrays.stream(CriticalityType.values()).forEach(c -> this.criticalities.put(c, Double.NEGATIVE_INFINITY));
    source.addOutgoingEdge(this);
    target.addIncomingEdge(this);
  }

  public Node getSource() {
    return this.source;
  }

  public Node getTarget() {
    return this.target;
  }

  public int getMaxCapacity() {
    return this.maxCapacity;
  }

  public int getExpectedEntitiesNumber() {
    return this.expectedEntitiesNumber;
  }

  public void setExpectedEntitiesNumber(int expectedEntitiesNumber) {
    this.expectedEntitiesNumber = expectedEntitiesNumber;
  }

  public double getCongestion() {
    return ((double) getMobileEntities().size()) / this.maxCapacity;
  }

  public double getCriticality(CriticalityType ctype) {
    return this.criticalities.get(ctype);
  }

  public Map<CriticalityType, Double> getCriticalities() {
    return new HashMap<>(this.criticalities);
  }

  public void setCriticality(CriticalityType ctype, double value) {
    if (!isObserved()) {
      this.criticalities.put(ctype, value);
    }
  }

  @Override
  protected double computeCriticality() {
    if (isObserved()) {
      computeCriticalities();
    }
    return super.computeCriticality();
  }

  private void computeCriticalities() {
    this.criticalities.put(CriticalityType.RATIO, (double) mobileEntitiesDifference() / this.expectedEntitiesNumber);
    this.criticalities.put(CriticalityType.DIFFERENCE, (double) mobileEntitiesDifference());
    this.criticalities.put(CriticalityType.OBSERVATION_ZONE, getObserver().getCriticality());
  }

  @Override
  public List<MobileEntity> getMobileEntities() {
    return this.amas.mobileEntities(me -> me.getCurrentEdge() == this);
  }

  public int mobileEntitiesDifference() {
    return getMobileEntities().size() - this.expectedEntitiesNumber;
  }

  @Override
  protected void onAct() {
    System.out.println(this + ":onAct"); // DEBUG
    Collection<UpdateEdgeCriticalitiesMessage> messages = getReceivedMessagesGivenType(
        UpdateEdgeCriticalitiesMessage.class);
    boolean updated = false;
    if (!isObserved() || !messages.isEmpty()) {
      System.out.println("Messages: " + messages); // DEBUG

      for (UpdateEdgeCriticalitiesMessage message : messages) {
        for (Map.Entry<Edge.CriticalityType, Double> entry : message.getCriticalities().entrySet()) {
          double newValue = entry.getValue();
          double previous = this.criticalities.put(entry.getKey(), newValue);
          if (previous != newValue) {
            updated = true;
          }
        }
      }
    }
    if (isObserved() || updated) {
      System.out.println("diff: " + mobileEntitiesDifference());
      if (mobileEntitiesDifference() != 0) {
        IAmakMessage message = new EdgeCriticalitiesMessage(this, this.criticalities);
        this.messageSent = sendMessage(message, getSource().getAID());
        this.messageSent |= sendMessage(message, getTarget().getAID());
        System.out.println("Message to nodes: " + message); // DEBUG
        System.out.println(this.amas.edges(Edge::hasSentMessage).size());
      }
    }
  };

  @Override
  public int compareTo(Edge e) {
    double epsilon = 1e-6;

    for (CriticalityType ctype : CriticalityType.values()) {
      double c1 = getCriticality(ctype);
      double c2 = e.getCriticality(ctype);
      if (Math.abs(c1 - c2) > epsilon) {
        return (int) Math.signum(c1 - c2);
      }
    }

    return 0;
  }

  public static enum CriticalityType {
    RATIO,
    DIFFERENCE,
    OBSERVATION_ZONE;
  }
}
