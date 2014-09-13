package org.jbpm.cmmn.flow.core;

import java.io.Serializable;

public interface PlanItemDefinition extends Serializable, CMMNElement {

	String getName();

	String getDescription();

	PlanItemControl getDefaultControl();

	void setDefaultControl(PlanItemControl c);

}
