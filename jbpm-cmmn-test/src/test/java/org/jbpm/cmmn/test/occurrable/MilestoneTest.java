package org.jbpm.cmmn.test.occurrable;

import java.util.List;

import org.jbpm.cmmn.instance.CaseInstance;
import org.jbpm.cmmn.instance.PlanElementState;
import org.junit.Test;
import org.kie.api.task.model.TaskSummary;

public class MilestoneTest extends AbstractOccurrableTestCase {
	public MilestoneTest() {
	}


	@Test
	public void testRepeat() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		// WHEN
		triggerOccurrence();
		// *****THEN
		assertPlanItemInState(caseInstance.getId(), "TheOccurrablePlanItem", PlanElementState.COMPLETED, 1);
		assertPlanItemInState(caseInstance.getId(), "TheRepeatableMilestonePlanItem", PlanElementState.COMPLETED, 1);
		assertPlanItemInState(caseInstance.getId(), "TheRepeatableMilestonePlanItem", PlanElementState.AVAILABLE);
		triggerOccurrence();
		assertPlanItemInState(caseInstance.getId(), "TheOccurrablePlanItem", PlanElementState.COMPLETED, 1);
		assertPlanItemInState(caseInstance.getId(), "TheRepeatableMilestonePlanItem", PlanElementState.COMPLETED, 2);
		assertPlanItemInState(caseInstance.getId(), "TheRepeatableMilestonePlanItem", PlanElementState.AVAILABLE);
		List<TaskSummary> tasks = getTaskService().getTasksAssignedAsPotentialOwner("Builder", "en-UK");
		assertTaskTypeCreated(tasks, "TaskEnteredOnOccurrenceOfOccurrablePlanItem", 1);
		assertTaskTypeCreated(tasks, "TaskEnteredOnOccurrenceOfRepeatablePlanItem", 2);
		assertNodeTriggered(caseInstance.getId(), "TaskEnteredOnOccurrenceOfOccurrablePlanItem");
		assertPlanItemInState(caseInstance.getId(), "TaskEnteredOnOccurrenceOfOccurrablePlanItem", PlanElementState.ENABLED);
	}

	@Override
	protected void triggerOccurrence() throws Exception {
		getPersistence().start();
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "onTheUserEventOccurPartId");
		caseInstance = (CaseInstance) getRuntimeEngine().getKieSession().getProcessInstance(caseInstance.getId());
		caseInstance.signalEvent("TheUserEvent", new Object());
		getPersistence().commitAndSendCaseFileItemEvents();
	}

	@Override
	public String getProcessFile() {
		return "test/occurrable/MilestoneTests.cmmn";
	}

	@Override
	public String getCaseName() {
		return "MilestoneTests";
	}
}
