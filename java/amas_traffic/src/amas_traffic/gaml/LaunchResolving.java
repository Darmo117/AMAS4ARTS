package amas_traffic.gaml;

import amas_traffic.amak.MASResolver;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.population.IPopulation;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gaml.descriptions.IDescription;
import msi.gaml.types.IType;

@symbol(name = "launch_amas_resolving", kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT })
@facets({ //
    @facet(name = "mobile_entities", type = IType.LIST, of = IType.AGENT, doc = @doc("The list of all alive mobile entities.")), //
})
@doc("This statement launches the resolving MAS.")
public final class LaunchResolving extends CustomStatement {
  public LaunchResolving(IDescription desc) {
    super(desc);
  }

  @Override
  protected Object privateExecuteIn(IScope scope) {
    IPopulation<IAgent> mobileEntities = getFacetValue_Cast(scope, "mobile_entities");
    checkPopulationSpecies(scope, mobileEntities, "mobile_entity");
    double timestamp = scope.getClock().getTimeElapsedInSeconds();
    System.out.println("LaunchResolving.privateExecuteIn timestamp: " + timestamp); // DEBUG

    MASResolver.getResolver().solve(timestamp, mobileEntities);
    System.out.println("======================================= stopped"); // DEBUG

    return null;
  }
}
