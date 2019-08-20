package amas_traffic.amak.agents.messaging;

import java.util.Map;

import amas_traffic.amak.agents.network.Edge;
import amas_traffic.amak.agents.network.Node;

public class UpdateEdgeCriticalitiesMessage extends MessageImpl<Node> {
  private final Map<Edge.CriticalityType, Double> criticalities;

  public UpdateEdgeCriticalitiesMessage(Node sender, Map<Edge.CriticalityType, Double> criticalities) {
    super(sender);
    this.criticalities = criticalities;
  }

  public Map<Edge.CriticalityType, Double> getCriticalities() {
    return this.criticalities;
  }
}
