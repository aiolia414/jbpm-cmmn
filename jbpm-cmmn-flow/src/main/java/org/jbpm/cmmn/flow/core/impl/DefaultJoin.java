package org.jbpm.cmmn.flow.core.impl;

import org.jbpm.workflow.core.node.EventNodeInterface;
import org.jbpm.workflow.core.node.Join;
@Deprecated()
/**
 * Refactor away
 */

public class DefaultJoin extends Join implements EventNodeInterface {

	private static final long serialVersionUID = 4769541084056401100L;
	public static final String CLOSE = "Close";

	@Override
	public boolean acceptsEvent(String type, Object event) {
		return type.equals(DefaultJoin.CLOSE);
	}

}
