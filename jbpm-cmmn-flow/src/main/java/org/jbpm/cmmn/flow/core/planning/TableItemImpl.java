package org.jbpm.cmmn.flow.core.planning;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.cmmn.flow.core.ApplicabilityRule;
import org.jbpm.cmmn.flow.core.Case;
import org.jbpm.cmmn.flow.core.CaseRole;
import org.jbpm.cmmn.flow.core.PlanningTable;
import org.jbpm.cmmn.flow.core.TableItem;
import org.jbpm.cmmn.flow.core.impl.AbstractItem;
import org.jbpm.cmmn.flow.core.impl.PlannerRoleCalculator;

public class TableItemImpl extends AbstractItem implements TableItem{
	private static final long serialVersionUID = 6743815602437868413L;
	private Map<String, CaseRole> authorizedRoles = new HashMap<String, CaseRole>();

	private PlanningTableImpl planningTable;
	private String description;
	private Map<String, ApplicabilityRule> applicabilityRules = new HashMap<String, ApplicabilityRule>();

	public void putApplicabilityRule(String id, ApplicabilityRule r) {
		this.applicabilityRules.put(id, r);
	}

	public Map<String, ApplicabilityRule> getApplicabilityRules() {
		return applicabilityRules;
	}

	public void putAuthorizedRole(String id, CaseRole role) {
		authorizedRoles.put(id, role);
	}

	@Override
	public Map<String, CaseRole> getAuthorizedRoles() {
		return authorizedRoles;
	}

	public static String getPlannerRoles(Case theCase) {
		return PlannerRoleCalculator.getPlannerRoles(theCase.getPlanningTable());
	}

	@Override
	public PlanningTable getParentTable() {
		return planningTable;
	}

	public void setParentTable(PlanningTableImpl planningTable) {
		this.planningTable = planningTable;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
