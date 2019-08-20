package amas_traffic.amak.agents;

import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.World;
import fr.irit.smac.amak.CommunicatingAgent;

/**
 * This class adds a name to agents inheriting from it.
 * 
 * @author Damien Vergnet
 */
public abstract class NamedAgent extends CommunicatingAgent<TrafficAmas, World> {
  public static int c = 0; // DEBUG
  private final String name;

  public NamedAgent(TrafficAmas amas, String name) {
    super(amas);
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public String toString() {
    return String.format("%s#%d", this.name, getId());
  }

  @Override
  protected void onAgentCycleBegin() {
    System.out.println(++c + " " + this.name);
  }
}
