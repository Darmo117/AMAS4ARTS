package amas_traffic.amak.agents;

import java.util.List;

import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.TrafficSummary;
import amas_traffic.amak.agents.network.Edge;
import amas_traffic.amak.agents.network.Node;

public class ObservationZone extends NamedAgent {
  private final TrafficSummary summary;
  private final List<Node> nodes;
  private final List<Edge> edges;

  public ObservationZone(TrafficAmas amas, String name, List<Node> nodes, List<Edge> edges, TrafficSummary summary) {
    super(amas, name);
    this.criticalities.put(this, 0.0);
    this.nodes = nodes;
    this.edges = edges;
    this.nodes.forEach(n -> n.setObserver(this));
    this.edges.forEach(e -> e.setObserver(this));
    this.summary = summary;
  }

  public double getCriticality() {
    return this.criticalities.get(this);
  }

  @Override
  protected double computeCriticality() {
    return this.edges.stream().mapToInt(e -> e.mobileEntitiesDifference() == 0 ? 0 : 1).sum();
  }

  @Override
  protected void onAct() {
    double timestamp = this.amas.getTimestamp();
    System.out.println(this + ".onAct"); // DEBUG
    System.out.println(timestamp);

    for (Edge edge : this.edges) {
      System.out.println("edge: " + edge.getName()); // DEBUG
      System.out.println("expected: " + this.summary.getMobileEntitiesCount(timestamp, edge.getName()));
      edge.setExpectedEntitiesNumber(this.summary.getMobileEntitiesCount(timestamp, edge.getName()));
    }
  }

  @Override
  public int getExecutionOrder() {
    return 1;
  }
}
