package org.jbpm.cmmn.flow.core.planitem;

import org.jbpm.cmmn.flow.core.ItemWithDefinition;
import org.jbpm.workflow.core.node.StateNode;

public class PlanItemInstanceFactoryNode extends StateNode {

	private static final long serialVersionUID = -3811996856528514976L;
	private ItemWithDefinition<?> planItem;

	public PlanItemInstanceFactoryNode() {
	}

	public ItemWithDefinition<?> getItemToInstantiate() {
		return planItem;
	}

	public void setItemToInstantiate(ItemWithDefinition<?> planItem) {
		this.planItem = planItem;
	}
}
