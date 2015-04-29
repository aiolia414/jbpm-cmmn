package org.jbpm.cmmn.test.controllable;

import org.jbpm.cmmn.instance.PlanElementState;
import org.jbpm.cmmn.instance.impl.StageInstance;
import org.kie.api.task.model.Status;

public class StageAsPlanItemTest extends AbstractControllableLifecycleTest {
	{
		super.isJpa = true;
	}

	public StageAsPlanItemTest() {
	}

	// @Test
	// public void testStageTriggered() throws Exception {
	// // *****GIVEN
	// givenThatTheTestCaseIsStarted();
	// triggerStartOfTask();
	// assertNodeTriggered(caseInstance.getId(), "TopLevelTask");
	// List<TaskSummary> list = getTaskService().getTasksAssignedAsPotentialOwner("Builder", "en-UK");
	// assertEquals(1, list.size());
	// getTaskService().start(list.get(0).getId(), "Builder");
	//
	// // *****WHEN
	// getTaskService().complete(list.get(0).getId(), "Builder", new HashMap<String,Object>());
	// // *****THEN
	// assertNodeTriggered(caseInstance.getId(), "TheStagePlanItem");
	// list = getTaskService().getTasksAssignedAsPotentialOwner("Administrator", "en-UK");
	// assertEquals(2, list.size()); //Tasks representing Stage and the Case
	// assertTaskTypeCreated(list, "TheStagePlanItem");
	// }

	@Override
	public void failTask(long taskId) {
		getPersistence().start();
		long wi = getTaskService().getTaskById(taskId).getTaskData().getWorkItemId();
		StageInstance spi = (StageInstance) reloadCaseInstance(caseInstance).findNodeForWorkItem(wi);
		spi.fault();
		getPersistence().commit();
	}

	@Override
	public void completeTask(long taskId) {
		getPersistence().start();
		getRuntimeEngine().getKieSession().signalEvent("StageCompletingEvent", new Object(), caseInstance.getId());
		getPersistence().commit();
		assertPlanItemInState(caseInstance.getId(), "TheMilestonePlanItem", PlanElementState.COMPLETED);
		assertEquals(Status.Completed, getTaskService().getTaskById(taskId).getTaskData().getStatus());
	}

	@Override
	public String getEventGeneratingTaskUser() {
		return "ConstructionProjectManager";
	}

	@Override
	protected String getCaseOwner() {
		return "Spielman";
	}

	@Override
	public String getNameOfProcessToStart() {
		return "StageTests";
	}

	@Override
	public String[] getProcessFileNames() {
		return new String[] { "test/controllable/StageAsPlanItemTests.cmmn" };
	}

}
