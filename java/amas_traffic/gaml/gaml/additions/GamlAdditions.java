package gaml.additions;
import msi.gaml.extensions.multi_criteria.*;
import msi.gama.outputs.layers.charts.*;
import msi.gama.outputs.layers.*;
import msi.gama.outputs.*;
import msi.gama.kernel.batch.*;
import msi.gama.kernel.root.*;
import msi.gaml.architecture.weighted_tasks.*;
import msi.gaml.architecture.user.*;
import msi.gaml.architecture.reflex.*;
import msi.gaml.architecture.finite_state_machine.*;
import msi.gaml.species.*;
import msi.gama.metamodel.shape.*;
import msi.gaml.expressions.*;
import msi.gama.metamodel.topology.*;
import msi.gaml.statements.test.*;
import msi.gama.metamodel.population.*;
import msi.gama.kernel.simulation.*;
import msi.gama.kernel.model.*;
import java.util.*;
import msi.gaml.statements.draw.*;
import  msi.gama.metamodel.shape.*;
import msi.gama.common.interfaces.*;
import msi.gama.runtime.*;
import java.lang.*;
import msi.gama.metamodel.agent.*;
import msi.gaml.types.*;
import msi.gaml.compilation.*;
import msi.gaml.factories.*;
import msi.gaml.descriptions.*;
import msi.gama.util.tree.*;
import msi.gama.util.file.*;
import msi.gama.util.matrix.*;
import msi.gama.util.graph.*;
import msi.gama.util.path.*;
import msi.gama.util.*;
import msi.gama.runtime.exceptions.*;
import msi.gaml.factories.*;
import msi.gaml.statements.*;
import msi.gaml.skills.*;
import msi.gaml.variables.*;
import msi.gama.kernel.experiment.*;
import msi.gaml.operators.*;
import msi.gama.common.interfaces.*;
import msi.gama.extensions.messaging.*;
import msi.gama.metamodel.population.*;
import msi.gaml.operators.Random;
import msi.gaml.operators.Maths;
import msi.gaml.operators.Points;
import msi.gaml.operators.Spatial.Properties;
import msi.gaml.operators.System;
import static msi.gaml.operators.Cast.*;
import static msi.gaml.operators.Spatial.*;
import static msi.gama.common.interfaces.IKeyword.*;
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })

public class GamlAdditions extends AbstractGamlAdditions {
	public void initialize() throws SecurityException, NoSuchMethodException {
	initializeSymbol();
	initializeOperator();
}public void initializeSymbol()  {
_symbol(S("init_amas"),amas_traffic.gaml.InitAmas.class,2,F,F,T,F,F,F,AS,I(3,11),P(_facet("road_graph",I(15),0,0,AS,F,F),_facet("nodes",I(5),11,0,AS,F,F),_facet("edges",I(5),11,0,AS,F,F),_facet("oz_file",I(12),0,0,AS,F,F),_facet("traffic_file",I(12),0,0,AS,F,F)),NAME,(x)->new amas_traffic.gaml.InitAmas(x));
_symbol(S("launch_amas_resolving"),amas_traffic.gaml.LaunchResolving.class,2,F,F,T,F,F,F,AS,I(3,11),P(_facet("mobile_entities",I(5),11,0,AS,F,F)),NAME,(x)->new amas_traffic.gaml.LaunchResolving(x));
}public void initializeOperator() throws SecurityException, NoSuchMethodException {
_operator(S("get_target_from_amas"),amas_traffic.gaml.GetPathFromAmas.class.getMethod("getTargetOrNextStep",S),C(S),AI,List.class,F,5,-13,-13,-13,(s,o)->amas_traffic.gaml.GetPathFromAmas.getTargetOrNextStep(((String)o[0])));
}
}