package amas_traffic.amak.agents.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.agents.MobileEntity;
import amas_traffic.amak.agents.messaging.EdgeCriticalitiesMessage;
import amas_traffic.amak.agents.messaging.UpdateEdgeCriticalitiesMessage;
import amas_traffic.amak.agents.network.Edge.CriticalityType;
import msi.gama.metamodel.shape.ILocation;

public class Node extends ObservableAgent {
  private final ILocation location;
  private List<Edge> incomingEdges;
  private List<Edge> outgoingEdges;
  private Edge mostCriticalLackingEdge;

  public Node(TrafficAmas amas, String name, ILocation location) {
    super(amas, name);
    this.location = location;
    this.incomingEdges = new ArrayList<>();
    this.outgoingEdges = new ArrayList<>();
  }

  public ILocation getLocation() {
    return this.location;
  }

  void addIncomingEdge(Edge edge) {
    this.incomingEdges.add(Objects.requireNonNull(edge));
  }

  void addOutgoingEdge(Edge edge) {
    this.outgoingEdges.add(Objects.requireNonNull(edge));
  }

  public Edge getMostCriticalLackingEdge() { // TEMP pas terrible
    return this.mostCriticalLackingEdge != null ? this.mostCriticalLackingEdge : this.outgoingEdges.get(0);
  }

  @Override
  protected void onAct() {
    Collection<EdgeCriticalitiesMessage> messages = getReceivedMessagesGivenType(EdgeCriticalitiesMessage.class);
    System.out.println(this + ":onAct"); // DEBUG
    System.out.println("Messages: " + messages);

    if (!isObserved() && !messages.isEmpty()) {
      Map<Edge, Map<Edge.CriticalityType, Double>> updatedEdges = new HashMap<>();
      for (EdgeCriticalitiesMessage message : messages) {
        updatedEdges.put(message.getSender(), message.getCriticalities());
      }
      List<Edge> edges = new ArrayList<>(updatedEdges.keySet());

      edges.sort((e1, e2) -> {
        double epsilon = 1e-6;

        for (CriticalityType ctype : CriticalityType.values()) {
          double c1 = Math.abs(updatedEdges.get(e1).get(ctype));
          double c2 = Math.abs(updatedEdges.get(e2).get(ctype));
          if (Math.abs(c1 - c2) > epsilon) {
            return (int) Math.signum(c2 - c1);
          }
        }

        return 0;
      });

      this.mostCriticalLackingEdge = edges.stream().filter(e -> this.outgoingEdges.contains(e)).findFirst().get();
      int diff = this.mostCriticalLackingEdge.mobileEntitiesDifference();
      int lack = -diff;

      List<Edge> selectedEdges = edges.stream().filter(e -> this.incomingEdges.contains(e) && !e.isObserved())
          .collect(Collectors.toList());

      for (Edge edge : selectedEdges) {
        Map<Edge.CriticalityType, Double> criticalities = new HashMap<>();

        if (diff >= 0 || lack <= 0) {
          criticalities.put(Edge.CriticalityType.RATIO, 0.0);
          criticalities.put(Edge.CriticalityType.DIFFERENCE, 0.0);
        }
        else {
          int mobileEntitiesNumber = edge.getMobileEntities().size();
          int capacity = edge.getMaxCapacity();
          int margin = capacity - mobileEntitiesNumber;
          int numberToSend = 0;

          lack -= mobileEntitiesNumber;
          if (margin >= lack) {
            numberToSend = lack;
            lack = 0;
          }
          else {
            numberToSend = margin;
            lack -= margin;
          }

          criticalities.put(Edge.CriticalityType.RATIO, -(double) numberToSend / mobileEntitiesNumber);
          criticalities.put(Edge.CriticalityType.DIFFERENCE, -(double) numberToSend);
        }

        this.messageSent = sendMessage(new UpdateEdgeCriticalitiesMessage(this, criticalities), edge.getAID());
      }
    }
  }

  public void redirectMobileEntity() {
    for (MobileEntity me : getMobileEntities()) {
      if (me != null && !me.hasTarget()) {
        me.setNextStep(getMostCriticalLackingEdge().getTarget());
      }
    }
  }

  @Override
  public List<MobileEntity> getMobileEntities() {
    return this.amas.mobileEntities(me -> me.getCurrentNode() == this);
  }
}
