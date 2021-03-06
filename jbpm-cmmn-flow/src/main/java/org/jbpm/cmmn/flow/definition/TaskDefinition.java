package org.jbpm.cmmn.flow.definition;

import org.jbpm.cmmn.flow.core.CaseParameter;
import org.kie.api.definition.process.*;

import java.util.Collection;
import java.util.List;

public interface TaskDefinition extends RepeatablePlanItemDefinition {
	Collection<CaseParameter> getInputs();

	Collection<CaseParameter> getOutputs();
	CaseParameter getInputParameter(String name);
	void addOutputParameter(CaseParameter cp);

	void addInputParameter(CaseParameter cp);

    boolean isBlocking();

}
