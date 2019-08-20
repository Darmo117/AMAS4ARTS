package amas_traffic.amak.utils;

public class DummyObservationZone {
  private final String name;
  private final String[] nodeIds;
  private final String[] edgeIds;

  public DummyObservationZone(String name, String[] nodeIds, String[] edgeIds) {
    this.name = name;
    this.nodeIds = nodeIds;
    this.edgeIds = edgeIds;
  }

  public String getName() {
    return this.name;
  }

  public String[] getNodeIds() {
    return this.nodeIds;
  }

  public String[] getEdgeIds() {
    return this.edgeIds;
  }
}
