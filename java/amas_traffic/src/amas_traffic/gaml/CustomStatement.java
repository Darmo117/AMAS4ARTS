package amas_traffic.gaml;

import msi.gama.metamodel.population.IPopulation;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.descriptions.IDescription;
import msi.gaml.statements.AbstractStatement;

/**
 * Base class for all statements defined in this plugin.
 * 
 * @author Damien Vergnet
 */
public abstract class CustomStatement extends AbstractStatement {
  public CustomStatement(IDescription desc) {
    super(desc);
  }

  /**
   * Returns the value of a facet and casts it to the desired type.
   * 
   * @param <T>   The type to cast the value into.
   * @param scope The current scope.
   * @param facet The facet's name.
   * @return The cast facet value.
   */
  @SuppressWarnings("unchecked")
  protected <T> T getFacetValue_Cast(IScope scope, String facet) {
    return (T) getFacetValue(scope, facet);
  }

  /**
   * Checks that a population contains Gama agents of a given species.
   * 
   * @param scope           The current scope
   * @param population      The population.
   * @param expectedSpecies The species name to check the population against.
   */
  protected static void checkPopulationSpecies(IScope scope, IPopulation<?> population, String expectedSpecies) {
    if (!population.getSpecies().getName().equals(expectedSpecies)) {
      String msg = String.format("expected population of <%s>, got <%s>", expectedSpecies,
          population.getSpecies().getName());
      throw GamaRuntimeException.error(msg, scope);
    }
  }
}
