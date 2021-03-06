package org.jbpm.cmmn.flow.core;

import org.jbpm.cmmn.flow.definition.PlanItemDefinition;
import org.jbpm.cmmn.flow.planitem.PlanItem;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.workflow.core.WorkflowProcess;

import java.util.Collection;

public interface Case extends PlanItemContainer,WorkflowProcess {
	String CASE_TYPE="Case";
	CaseParameter getInputParameter(String id);
	CaseParameter getOutputParameter(String id);
	Collection<CaseParameter> getInputParameters();
	PlanItemDefinition getPlanItemDefinition(String id);
	String getCaseKey();

}
