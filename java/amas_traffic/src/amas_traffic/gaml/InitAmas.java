package amas_traffic.gaml;

import java.util.List;

import amas_traffic.amak.MASResolver;
import amas_traffic.amak.TrafficSummary;
import amas_traffic.amak.utils.DummyObservationZone;
import amas_traffic.amak.utils.FileUtils;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.population.IPopulation;
import msi.gama.metamodel.shape.ILocation;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.graph.IGraph;
import msi.gaml.descriptions.IDescription;
import msi.gaml.types.IType;

@symbol(name = "init_amas", kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT })
@facets({ //
    @facet(name = "road_graph", type = IType.GRAPH, doc = @doc("The current road_graph. It is assumed to stay the same throughout the whole simulation.")), //
    @facet(name = "nodes", type = IType.LIST, of = IType.AGENT, doc = @doc("The list of all node agents.")), //
    @facet(name = "edges", type = IType.LIST, of = IType.AGENT, doc = @doc("The list of all edge agents.")), //
    @facet(name = "oz_file", type = IType.FILE, doc = @doc("The file defining observation zones.")), //
    @facet(name = "traffic_file", type = IType.FILE, doc = @doc("The file containing all traffic data.")), //
})
@doc("This statement launches the resolving MAS.")
public final class InitAmas extends CustomStatement {
  public InitAmas(IDescription desc) {
    super(desc);
  }

  @Override
  protected Object privateExecuteIn(IScope scope) throws GamaRuntimeException {
    IGraph<ILocation, IAgent> graph = getFacetValue_Cast(scope, "road_graph");
    IPopulation<IAgent> nodes = getFacetValue_Cast(scope, "nodes");
    IPopulation<IAgent> edges = getFacetValue_Cast(scope, "edges");
    List<DummyObservationZone> dummyOZs = FileUtils.getObservationZones(getFacetValue_Cast(scope, "oz_file"), scope);
    TrafficSummary trafficData = FileUtils.extractTrafficData(getFacetValue_Cast(scope, "traffic_file"), scope);

    checkPopulationSpecies(scope, nodes, "graph_vertex");
    checkPopulationSpecies(scope, edges, "graph_link");

    MASResolver.init(graph, nodes, edges, dummyOZs, trafficData);

    return null;
  }
}
