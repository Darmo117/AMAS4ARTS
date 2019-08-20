package amas_traffic.gaml;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import amas_traffic.amak.MASResolver;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gaml.types.IType;

public class GetPathFromAmas {
  @operator(value = "get_target_from_amas", type = IType.LIST, doc = @doc("Returns the target for the given mobile entity ID. Result may be null."))
  public static List<Object> getTargetOrNextStep(String entityName) {
    Pair<String, Boolean> target = MASResolver.getResolver().getTargetOrNextStepForMobileEntity(entityName);
    System.out.println(target);
    if (target != null) {
      List<Object> l = new ArrayList<>();
      l.add(target.getKey());
      l.add(target.getValue());
      System.out.println(l);
      return l;
    }
    System.out.println("null");
    return null;
  }
}
