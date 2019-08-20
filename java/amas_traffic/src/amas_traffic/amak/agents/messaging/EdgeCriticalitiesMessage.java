package amas_traffic.amak.agents.messaging;

import java.util.Map;

import amas_traffic.amak.agents.network.Edge;

public class EdgeCriticalitiesMessage extends MessageImpl<Edge> {
  private final Map<Edge.CriticalityType, Double> criticalities;

  public EdgeCriticalitiesMessage(Edge sender, Map<Edge.CriticalityType, Double> criticalities) {
    super(sender);
    this.criticalities = criticalities;
  }

  public Map<Edge.CriticalityType, Double> getCriticalities() {
    return this.criticalities;
  }
}
