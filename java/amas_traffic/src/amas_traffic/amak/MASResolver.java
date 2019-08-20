package amas_traffic.amak;

import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math3.util.Pair;

import amas_traffic.amak.agents.MobileEntity;
import amas_traffic.amak.agents.network.Node;
import amas_traffic.amak.utils.DummyObservationZone;
import amas_traffic.amak.utils.FileUtils;
import fr.irit.smac.amak.Configuration;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.ILocation;
import msi.gama.util.graph.IGraph;

public final class MASResolver {
  private static MASResolver resolver;

  public static synchronized void init(IGraph<ILocation, IAgent> graph, List<IAgent> nodeAgents,
      List<IAgent> edgeAgents, List<DummyObservationZone> dummyOZs, TrafficSummary summary) {
    System.out.println("======================================= init"); // DEBUG
    if (resolver != null) {
      resolver.dispose();
    }
    resolver = new MASResolver(graph, nodeAgents, edgeAgents, dummyOZs, summary);
  }

  public static MASResolver getResolver() {
    if (resolver == null) {
      throw new IllegalStateException("resolver not initialized");
    }
    return resolver;
  }

  private TrafficAmas amas;

//  private IGraph<ILocation, IAgent> graph;
//  private List<IAgent> nodeAgents;
//  private List<IAgent> edgeAgents;
//  private List<DummyObservationZone> dummyOZs;
//  private TrafficSummary summary;

  private MASResolver(IGraph<ILocation, IAgent> graph, List<IAgent> nodeAgents, List<IAgent> edgeAgents,
      List<DummyObservationZone> dummyOZs, TrafficSummary summary) {
    Configuration.commandLineMode = true;
    this.amas = new TrafficAmas(graph, nodeAgents, edgeAgents, dummyOZs, summary);
//    this.graph = graph;
//    this.nodeAgents = nodeAgents;
//    this.edgeAgents = edgeAgents;
//    this.dummyOZs = dummyOZs;
//    this.summary = summary;
  }

  public void solve(double timestamp, List<IAgent> mobileEntities) {
//    this.amas = new TrafficAmas(this.graph, this.nodeAgents, this.edgeAgents, this.dummyOZs, this.summary); // TEMP
    this.amas.update(timestamp, mobileEntities);
    this.amas.start();
    try {
      System.out.println("c " + this.amas.getSemaphore().availablePermits()); // DEBUG
      this.amas.getSemaphore().acquire();
      System.out.println("MASResolver.solve: semaphore acquired"); // DEBUG
      System.out.println("d " + this.amas.getSemaphore().availablePermits()); // DEBUG
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("MASResolver.solve: semaphore released"); // DEBUG
    this.amas.getSemaphore().release();

    String path = Paths.get("D:\\eclipse-workspace\\amas_traffic\\out", "%%s-%f.csv").toString();
    FileUtils.writeCSV(this.amas, String.format(Locale.ENGLISH, path, timestamp)); // TEMP
  }

  /**
   * Returns the next node or the target the given mobile entity has to reach.
   * 
   * @param name Mobile entity's name.
   * @return The next step or target. The target is only returned once.
   */
  public Pair<String, Boolean> getTargetOrNextStepForMobileEntity(String name) {
    System.out.println("MASResolver.getTargetOrNextStepForMobileEntity name: " + name); // DEBUG
    MobileEntity me = this.amas.getNamedAgent(name);
    Node nextStep = me.getNextStep();
    Node target = me.getTarget();
    System.out.println("MASResolver.getTargetOrNextStepForMobileEntity me.getCurrentNode: " + me.getCurrentNode()); // DEBUG

    if (nextStep != null) {
      System.out.println("MASResolver.getTargetOrNextStepForMobileEntity nextStep: " + nextStep.getName()); // DEBUG
      return new Pair<>(nextStep.getName(), false);
    }
    else if (target != null && !me.hasReturnedTarget()) {
      me.setTargetReturned();
      System.out.println("MASResolver.getTargetOrNextStepForMobileEntity target: " + target.getName()); // DEBUG
      return new Pair<>(target.getName(), true);
    }
    return null;
  }

  public void dispose() {
    this.amas.dispose();
  }
}
