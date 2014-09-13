package org.jbpm.cmmn.flow.core.planitem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.drools.core.process.core.ParameterDefinition;
import org.drools.core.process.core.Work;
import org.drools.core.process.core.impl.ParameterDefinitionImpl;
import org.drools.core.process.core.impl.WorkImpl;
import org.jbpm.cmmn.common.WorkItemParameters;
import org.jbpm.cmmn.flow.core.Case;
import org.jbpm.cmmn.flow.core.PlanItemContainer;
import org.jbpm.cmmn.flow.core.PlanItemInfo;
import org.jbpm.cmmn.flow.core.PlanningTable;
import org.jbpm.cmmn.flow.core.TaskItemWithDefinition;
import org.jbpm.cmmn.flow.core.impl.PlannerRoleCalculator;
import org.jbpm.cmmn.flow.core.impl.Stage;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.kie.api.definition.process.Node;

public class StagePlanItem extends AbstractPlanItem<Stage> implements MultiInstancePlanItem, PlanItemContainer, TaskItemWithDefinition<Stage> {
	private static final long serialVersionUID = -4998194330899363230L;
	private PlanItemInstanceFactoryNode factoryNode;

	private StartNode defaultStart;
	private Split defaultSplit;
	private EndNode defaultEnd;
	private Join defaultJoin;
	private PlanningTable planningTable;
	private boolean autoComplete;

	public StagePlanItem() {
	}

	public StagePlanItem(PlanItemInfoImpl<Stage> info, PlanItemInstanceFactoryNode planItemInstanceFactoryNode) {
		super(info);
		this.factoryNode = planItemInstanceFactoryNode;
	}

	@Override
	public Node superGetNode(long id) {
		return super.getNode(id);
	}

	public StartNode getDefaultStart() {
		return defaultStart;
	}

	public void setDefaultStart(StartNode defaultStart) {
		this.defaultStart = defaultStart;
	}

	@Override
	public Node getNode(long id) {
		return PlanItemContainerUtil.getNode(this, id);
	}

	public Split getDefaultSplit() {
		return defaultSplit;
	}

	public void setDefaultSplit(Split defaultSplit) {
		this.defaultSplit = defaultSplit;
	}

	public EndNode getDefaultEnd() {
		return defaultEnd;
	}

	public void setDefaultEnd(EndNode defaultEnd) {
		this.defaultEnd = defaultEnd;
	}

	public Join getDefaultJoin() {
		return defaultJoin;
	}

	public void setDefaultJoin(Join defaultJoin) {
		this.defaultJoin = defaultJoin;
	}

	public PlanningTable getPlanningTable() {
		return planningTable;
	}

	public void setPlanningTable(PlanningTable planningTable) {
		this.planningTable = planningTable;
	}

	public void copyFromStage() {
		HashMap<Object, Object> copiedState = new HashMap<Object, Object>();
		Stage stage = getDefinition();
		copiedState.put(stage, this);
		copy(copiedState, stage, this);
		this.autoComplete = stage.isAutoComplete();
		this.defaultStart = copy(copiedState, stage.getDefaultStart());
		this.defaultSplit = copy(copiedState, stage.getDefaultSplit());
		this.defaultJoin = copy(copiedState, stage.getDefaultJoin());
		this.defaultEnd = copy(copiedState, stage.getDefaultEnd());
		this.planningTable = (PlanningTable) copy(copiedState, stage.getPlanningTable());
	}

	public Work getWork() {
		WorkImpl work = new WorkImpl();
		Work sourceWork = getDefinition().getWork();
		work.setName(sourceWork.getName());
		for (ParameterDefinition pd : sourceWork.getParameterDefinitions()) {
			work.addParameterDefinition(new ParameterDefinitionImpl(pd.getName(), pd.getType()));
		}
		for (Entry<String, Object> entry : sourceWork.getParameters().entrySet()) {
			work.setParameter(entry.getKey(), entry.getValue());
		}
		work.setParameter(WorkItemParameters.BUSINESSADMINISTRATOR_ID, getPlannerRoles(this));
		work.setParameter(WorkItemParameters.GROUP_ID, getPlannerRoles(this));
		work.setParameter("NodeName", getName());
		return work;
	}

	public static String getPlannerRoles(StagePlanItem spi) {
		return PlannerRoleCalculator.getPlannerRoles(spi.getDefinition().getPlanningTable(), spi.getPlanItemContainer().getPlanningTable());
	}

	@Override
	public PlanItemInstanceFactoryNode getFactoryNode() {
		return factoryNode;
	}

	@Override
	public void addPlanItemInfo(PlanItemInfo<?> d) {
	}

	@Override
	public Collection<PlanItemInfo<?>> getPlanItemInfo() {
		return this.getDefinition().getPlanItemInfo();
	}

	@Override
	public Case getCase() {
		return this.getDefinition().getCase();
	}

	@Override
	public boolean isAutoComplete() {
		return autoComplete;
	}
}
