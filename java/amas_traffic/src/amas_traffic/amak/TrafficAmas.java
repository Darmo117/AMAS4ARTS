package amas_traffic.amak;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import amas_traffic.amak.agents.MobileEntity;
import amas_traffic.amak.agents.NamedAgent;
import amas_traffic.amak.agents.ObservationZone;
import amas_traffic.amak.agents.network.Edge;
import amas_traffic.amak.agents.network.Node;
import amas_traffic.amak.agents.network.ObservableAgent;
import amas_traffic.amak.utils.DummyObservationZone;
import fr.irit.smac.amak.Agent;
import fr.irit.smac.amak.Amas;
import fr.irit.smac.amak.Scheduler;
import fr.irit.smac.amak.Scheduling;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.ILocation;
import msi.gama.util.graph.IGraph;

public class TrafficAmas extends Amas<World> {
  private Semaphore semaphore;
  private double timestamp;

  public TrafficAmas(IGraph<ILocation, IAgent> graph, List<IAgent> nodeAgents, List<IAgent> edgeAgents,
      List<DummyObservationZone> dummyOZs, TrafficSummary summary) {
    super(new World(), Scheduling.DEFAULT, graph, nodeAgents, edgeAgents, dummyOZs, summary);
    getScheduler().setOnStop(this::onStop);
    this.semaphore = new Semaphore(1);
  }

  public synchronized Semaphore getSemaphore() {
    return this.semaphore;
  }

  public double getTimestamp() {
    return this.timestamp;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void onInitialAgentsCreation() {
    IGraph<ILocation, IAgent> graph = (IGraph<ILocation, IAgent>) this.params[0];
    List<IAgent> nodeAgents = (List<IAgent>) this.params[1];
    List<IAgent> edgeAgents = (List<IAgent>) this.params[2];
    List<DummyObservationZone> dummyOZs = (List<DummyObservationZone>) this.params[3];
    TrafficSummary summary = (TrafficSummary) this.params[4];
    Map<ILocation, Node> locationToNode = new HashMap<>();
    Map<String, Node> nameToNode = new HashMap<>();
    Map<String, Edge> nameToEdge = new HashMap<>();

    for (IAgent nodeAgent : nodeAgents) {
      String name = nodeAgent.getName();
      ILocation location = nodeAgent.getLocation();
      Node node = new Node(this, name, location);
      locationToNode.put(location, node);
      nameToNode.put(name, node);
    }
    for (IAgent edgeAgent : edgeAgents) {
      ILocation sourceVertex = graph.getEdgeSource(edgeAgent);
      ILocation targetVertex = graph.getEdgeTarget(edgeAgent);
      String name = edgeAgent.getName();
      Node source = locationToNode.get(sourceVertex);
      Node target = locationToNode.get(targetVertex);
      int maxCapacity = (Integer) edgeAgent.getAttribute("max_capacity");
      nameToEdge.put(name, new Edge(this, name, source, target, maxCapacity));
    }
    for (DummyObservationZone doz : dummyOZs) {
      List<Node> nodes = Arrays.stream(doz.getNodeIds()).map(id -> nameToNode.get(id)).collect(Collectors.toList());
      List<Edge> edges = Arrays.stream(doz.getEdgeIds()).map(id -> nameToEdge.get(id)).collect(Collectors.toList());
      String[] edgeNames = edges.stream().map(e -> e.getName()).toArray(String[]::new);
      new ObservationZone(this, doz.getName(), nodes, edges, summary.getForEdges(edgeNames));
    }
  }

  public void update(double timestamp, List<IAgent> mobileEntities) {
    this.timestamp = timestamp;
    // Execution order of nodes and edges is different each time.
    agents(ObservableAgent.class, null).forEach(ObservableAgent::resetExecutionOrder);

    for (IAgent mobileEntityGama : mobileEntities) {
      String name = mobileEntityGama.getName();
      IAgent e = (IAgent) mobileEntityGama.getAttribute("current_edge");
      boolean isOnNode = (Boolean) mobileEntityGama.getAttribute("is_on_node");
      Edge currentEdge = e != null ? getNamedAgent(e.getName()) : null;
      IAgent n = (IAgent) mobileEntityGama.getAttribute("current_node");
      ILocation currentNodeLocation = n != null ? n.getLocation() : null;
      Node currentNode = isOnNode ? nodes(nn -> nn.getLocation().equals(currentNodeLocation)).get(0) : null;
      MobileEntity mobileEntity;

      if (mobileEntities(me -> me.getName().equals(name)).isEmpty()) {
        mobileEntity = new MobileEntity(this, name);
      }
      else {
        mobileEntity = mobileEntities(me -> me.getName().equals(name)).get(0);
      }
      mobileEntity.setCurrentEdge(currentEdge);
      mobileEntity.setCurrentNode(currentNode);
    }

    try {
      System.out.println("a1 " + this.semaphore.availablePermits()); // DEBUG
      this.semaphore.acquire();
      System.out.println("TrafficAmas.update: semaphore acquired");
      System.out.println("b1 " + this.semaphore.availablePermits());
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  protected void onSystemCycleBegin() {
    NamedAgent.c = 0;
    System.out.println("======================================= cycle " + getCycle() + " begin"); // DEBUG
    agents(ObservableAgent.class, ObservableAgent::hasSentMessage).forEach(ObservableAgent::resetMessageSent);
  }

  @Override
  protected void onSystemCycleEnd() {
    System.out.println("======================================= cycle " + getCycle() + " end"); // DEBUG
  }

  private void onStop(Scheduler s) {
    System.out.println("======================================= stop"); // DEBUG
    nodes(n -> !n.getMobileEntities().isEmpty()).forEach(Node::redirectMobileEntity);
    System.out.println("a " + this.semaphore.availablePermits());
    this.semaphore.release();
    System.out.println("TrafficAmas.onStop: semaphore released");
    System.out.println("b " + this.semaphore.availablePermits());
  }

  @Override
  public boolean stopCondition() {
    boolean stop = agents(ObservableAgent.class, ObservableAgent::hasSentMessage).isEmpty();
    System.out.println(edges(Edge::hasSentMessage).size());
    System.out.println("TrafficAmas.stopCondition stop: " + stop); // DEBUG
    return stop;
  }

  public List<Node> nodes(Predicate<Node> filter) {
    return agents(Node.class, filter);
  }

  public List<Edge> edges(Predicate<Edge> filter) {
    return agents(Edge.class, filter);
  }

  public List<ObservationZone> observationZones(Predicate<ObservationZone> filter) {
    return agents(ObservationZone.class, filter);
  }

  public List<MobileEntity> mobileEntities(Predicate<MobileEntity> filter) {
    return agents(MobileEntity.class, filter);
  }

  private synchronized <T extends NamedAgent> List<T> agents(Class<T> c, Predicate<T> filter) {
    Stream<T> stream = getAgents().stream().filter(c::isInstance).map(c::cast);
    if (filter != null) {
      stream = stream.filter(filter);
    }
    return stream.collect(Collectors.toList());
  }

  /**
   * Returns the agent with the given name.
   * 
   * @param <T>  The type the agent is expected to have.
   * @param name Agent's name.
   * @return The agent or null if none match the name.
   */
  @SuppressWarnings("unchecked")
  public <T extends NamedAgent> T getNamedAgent(String name) {
    return getAgents().stream().filter(a -> ((NamedAgent) a).getName().equals(name)).map(a -> (T) a).findFirst().get();
  }

  public void dispose() {
    getAgents().forEach(Agent::destroy);
  }
}
