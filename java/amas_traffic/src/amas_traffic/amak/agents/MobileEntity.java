package amas_traffic.amak.agents;

import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.agents.network.Edge;
import amas_traffic.amak.agents.network.Node;

public class MobileEntity extends NamedAgent {
  private Node currentNode;
  private Edge currentEdge;

  private Node target;
  private Node nextStep;
  private boolean targetReturned;

  public MobileEntity(TrafficAmas amas, String name) {
    super(amas, name);
    this.targetReturned = false;
  }

  public Node getCurrentNode() {
    return this.currentNode;
  }

  public void setCurrentNode(Node currentNode) {
    this.currentNode = currentNode;
  }

  public Edge getCurrentEdge() {
    return this.currentEdge;
  }

  public void setCurrentEdge(Edge currentEdge) {
    this.currentEdge = currentEdge;
  }

  public Node getTarget() {
    return this.target;
  }

  public void setTarget(Node target) {
    this.target = target;
  }

  public boolean hasTarget() {
    return this.target != null;
  }

  public Node getNextStep() {
    return this.nextStep;
  }

  public void setNextStep(Node nextStep) {
    this.nextStep = nextStep;
  }

  public boolean hasReturnedTarget() {
    return this.targetReturned;
  }

  public void setTargetReturned() {
    this.targetReturned = true;
  }

  @Override
  public int getExecutionOrder() {
    return 1_000_000;
  }
}
