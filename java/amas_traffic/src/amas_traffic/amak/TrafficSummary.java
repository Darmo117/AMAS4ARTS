package amas_traffic.amak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TrafficSummary extends TreeMap<Double, Map<String, List<MobileEntitySnapshot>>> {
  private static final long serialVersionUID = -6194889064664277528L;

  public TrafficSummary() {
    super(Double::compare);
  }

  /**
   * Returns a traffic summary containing only information for the given edges.
   * 
   * @param edgeNames Names of all edges.
   * @return Traffic data for all given edges.
   */
  public TrafficSummary getForEdges(String... edgeNames) {
    TrafficSummary forEdges = new TrafficSummary();

    for (Map.Entry<Double, Map<String, List<MobileEntitySnapshot>>> entry : entrySet()) {
      Map<String, List<MobileEntitySnapshot>> entries = new HashMap<>();
      for (String edgeName : edgeNames) {
        entries.put(edgeName, entry.getValue().get(edgeName));
      }
      forEdges.put(entry.getKey(), entries);
    }

    return forEdges;
  }

  public int getMobileEntitiesCount(double timestamp, String edgeName) {
    return get(floorKey(timestamp)).get(edgeName).size();
  }
}
