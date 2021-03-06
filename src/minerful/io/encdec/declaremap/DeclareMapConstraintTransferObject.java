package minerful.io.encdec.declaremap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import minerful.concept.TaskChar;
import minerful.concept.constraint.Constraint;
import minerful.concept.constraint.relation.RelationConstraint;

public class DeclareMapConstraintTransferObject {
	public final DeclareMapTemplate template;
	public final List<Set<String>> parameters;
	
	public DeclareMapConstraintTransferObject(Constraint con) {
		DeclareMapTemplateTranslator declaTemplaTranslo = new DeclareMapTemplateTranslator();
		this.template = declaTemplaTranslo.translateTemplateName(con.getClass());
		
		Set<String>
			firstParamSet = new TreeSet<String>(),
			secondParamSet = new TreeSet<String>();
		
		for (TaskChar tChars : con.getBase().getTaskCharsArray()) {
			firstParamSet.add(tChars.taskClass.getName());
		}
		
		if (con instanceof RelationConstraint) {
			RelationConstraint relaCon = (RelationConstraint) con;
			for (TaskChar tChars : relaCon.getImplied().getTaskCharsArray()) {
				secondParamSet.add(tChars.taskClass.getName());
			}
		}

		this.parameters = new ArrayList<Set<String>>();
		this.parameters.add(firstParamSet);
		if (!secondParamSet.isEmpty())
			this.parameters.add(secondParamSet);
	}
}